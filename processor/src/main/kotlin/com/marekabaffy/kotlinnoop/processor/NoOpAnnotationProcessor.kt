package com.marekabaffy.kotlinnoop.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.symbol.Visibility
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo

private const val NO_OP_ANNOTATION = "com.marekabaffy.kotlinnoop.annotations.NoOp"

/**
 * KSP processor that generates no-operation implementations for @NoOp annotated classes.
 */
class NoOpAnnotationProcessor(
    environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    /** Declarations by generated file, compared ignoring case because some file systems do. */
    private val generatedFiles = mutableMapOf<String, String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, deferred) = resolver.getSymbolsWithAnnotation(NO_OP_ANNOTATION)
            .partition { it.validate(enableNewFeatures = true) }

        valid.filterIsInstance<KSClassDeclaration>().forEach(::generate)

        return deferred
    }

    private fun generate(annotated: KSClassDeclaration) {
        val target = if (annotated.isCompanionObject) {
            annotated.parentDeclaration as KSClassDeclaration
        } else {
            annotated
        }
        val unsupportedReason = unsupportedReason(annotated, target)
        if (unsupportedReason != null) {
            logger.error("@NoOp $unsupportedReason", annotated)
            return
        }

        val flatName = target.toClassName().simpleNames.joinToString("")
        val builder = if (annotated.isCompanionObject) {
            companionFile(annotated, target, flatName)
        } else {
            directFile(target, flatName)
        } ?: return

        val file = builder
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .addMember("%S", "unused")
                    .build()
            )
            .build()

        val declaration = target.toClassName().canonicalName
        val collision = generatedFiles.putIfAbsent("${file.packageName}.${file.name}".lowercase(), declaration)
        if (collision != null) {
            logger.error(
                "@NoOp generates '${file.name}' for both $collision and $declaration. Rename one of them.",
                annotated
            )
            return
        }
        file.writeTo(codeGenerator, Dependencies(true, target.containingFile!!))
    }

    /** Returns why [target], reached through [annotated], cannot have a no-op, or null when it can. */
    private fun unsupportedReason(annotated: KSClassDeclaration, target: KSClassDeclaration): String? {
        val enclosing = generateSequence<KSDeclaration>(target) { it.parentDeclaration }
        return when {
            target.classKind != ClassKind.INTERFACE && target.classKind != ClassKind.CLASS ->
                "is only applicable to interfaces, classes and their companion objects."

            target.typeParameters.isNotEmpty() ->
                "does not support generic declarations, because the no-op has no type arguments to use."

            enclosing.any { it.getVisibility() == Visibility.PRIVATE || it.getVisibility() == Visibility.PROTECTED } ->
                "does not support private or protected declarations, because generated code cannot access them."

            target.classKind == ClassKind.INTERFACE && annotated.isCompanionObject && Modifier.SEALED in target.modifiers ->
                "does not support companion objects of sealed interfaces, because an anonymous object cannot " +
                    "extend one. Annotate the interface instead."

            target.classKind == ClassKind.INTERFACE -> null

            Modifier.ABSTRACT in target.modifiers || Modifier.SEALED in target.modifiers ->
                "does not support abstract or sealed classes, because they cannot be instantiated."

            Modifier.INNER in target.modifiers ->
                "does not support inner classes, because they need an outer instance."

            target.primaryConstructor == null -> "on a class requires a primary constructor."

            target.primaryConstructor?.getVisibility().let { it == Visibility.PRIVATE || it == Visibility.PROTECTED } ->
                "does not support classes with a private or protected primary constructor."

            else -> null
        }
    }

    /** Builds `object NoOpName : Name` for an interface, or `val NoOpName = Name(...)` for a class. */
    private fun directFile(target: KSClassDeclaration, flatName: String): FileSpec.Builder? {
        val packageName = target.packageName.asString()
        val generatedName = "NoOp$flatName"
        val builder = FileSpec.builder(packageName, generatedName)

        if (target.classKind == ClassKind.INTERFACE) {
            val noOpObject = TypeSpec.objectBuilder(generatedName)
                .addModifiers(generatedVisibility(target))
                .addSuperinterface(target.toClassName())
                .addNoOpMembers(target)
                .build()
            return builder.addType(noOpObject)
        }

        val constructorCall = constructorCall(target) ?: return null
        val noOpValue = PropertySpec.builder(generatedName, target.toClassName(), generatedVisibility(target))
            .initializer(constructorCall)
            .build()
        return builder.addProperty(noOpValue)
    }

    /** Builds `val Name.Companion.NoOp: Name`. */
    private fun companionFile(
        companion: KSClassDeclaration,
        target: KSClassDeclaration,
        flatName: String
    ): FileSpec.Builder? {
        val targetType = target.toClassName()
        val value = if (target.classKind == ClassKind.INTERFACE) {
            val anonymous = TypeSpec.anonymousClassBuilder()
                .addSuperinterface(targetType)
                .addNoOpMembers(target)
                .build()
            CodeBlock.of("return·%L\n", anonymous)
        } else {
            val constructorCall = constructorCall(target) ?: return null
            CodeBlock.of("return·%L\n", constructorCall)
        }
        val noOp = PropertySpec.builder("NoOp", targetType, generatedVisibility(target))
            .receiver(companion.toClassName())
            .getter(FunSpec.getterBuilder().addCode(value).build())
            .build()
        return FileSpec.builder(target.packageName.asString(), "${flatName}CompanionNoOp")
            .addProperty(noOp)
    }

    /** The generated no-op is internal when the target or anything enclosing it is internal. */
    private fun generatedVisibility(target: KSClassDeclaration): KModifier {
        val enclosing = generateSequence<KSDeclaration>(target) { it.parentDeclaration }
        return if (enclosing.any { it.getVisibility() == Visibility.INTERNAL }) {
            KModifier.INTERNAL
        } else {
            KModifier.PUBLIC
        }
    }

    private fun TypeSpec.Builder.addNoOpMembers(target: KSClassDeclaration): TypeSpec.Builder = apply {
        val owner = target.asType(emptyList())
        target.getAllProperties()
            .filter { it.getVisibility() != Visibility.PRIVATE }
            .forEach { addProperty(noOpProperty(it, owner)) }
        target.getAllFunctions()
            .filter { it.getVisibility() != Visibility.PRIVATE && !it.isAnyMember() }
            .forEach { addFunction(noOpFunction(it, owner)) }
    }

    /** Members inherited from `Any` keep their behaviour; only an interface's own redeclaration needs an override. */
    private fun KSFunctionDeclaration.isAnyMember(): Boolean =
        (parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString() == "kotlin.Any"

    private fun noOpProperty(property: KSPropertyDeclaration, owner: KSType): PropertySpec {
        val resolver = property.typeParameters.toTypeParameterResolver()
        val type = property.asMemberOf(owner)
        val typeName = typeName(type, resolver)
        val builder = PropertySpec.builder(property.simpleName.asString(), typeName, KModifier.OVERRIDE)
            .addTypeVariables(property.typeParameters.map { it.toTypeVariableName(resolver) })
            .getter(FunSpec.getterBuilder().addCode(returnStatement(type, resolver)).build())

        property.extensionReceiver?.let { builder.receiver(typeName(it.resolve(), resolver)) }

        if (property.isMutable) {
            builder.mutable()
            builder.setter(FunSpec.setterBuilder().addParameter("value", typeName).build())
        }
        return builder.build()
    }

    private fun noOpFunction(function: KSFunctionDeclaration, owner: KSType): FunSpec {
        val resolver: TypeParameterResolver = function.typeParameters.toTypeParameterResolver()
        val member = function.asMemberOf(owner)
        val returnType = member.returnType!!
        val builder = FunSpec.builder(function.simpleName.asString())
            .addModifiers(KModifier.OVERRIDE)
            .addTypeVariables(function.typeParameters.map { it.toTypeVariableName(resolver) })
            .returns(typeName(returnType, resolver))

        if (Modifier.SUSPEND in function.modifiers) builder.addModifiers(KModifier.SUSPEND)

        member.extensionReceiverType?.let { builder.receiver(typeName(it, resolver)) }

        function.parameters.zip(member.parameterTypes).forEach { (parameter, type) ->
            val modifiers = if (parameter.isVararg) listOf(KModifier.VARARG) else emptyList()
            builder.addParameter(
                ParameterSpec.builder(parameter.name!!.asString(), typeName(type!!, resolver), modifiers).build()
            )
        }

        when {
            function.isEqualsRedeclaration() -> builder.addStatement("return·this·===·%N", function.parameters.single().name!!.asString())
            returnType.declaration.qualifiedName?.asString() != "kotlin.Unit" || returnType.isMarkedNullable ->
                builder.addCode(returnStatement(returnType, resolver))
        }
        return builder.build()
    }

    /** `equals` must stay reflexive, so a no-op that redeclares it compares by identity instead of returning false. */
    private fun KSFunctionDeclaration.isEqualsRedeclaration(): Boolean =
        simpleName.asString() == "equals" &&
            parameters.singleOrNull()?.type?.resolve()?.let {
                it.isMarkedNullable && it.declaration.qualifiedName?.asString() == "kotlin.Any"
            } == true

    /**
     * Calls the primary constructor with a default for each parameter. A parameter with no known default
     * keeps its own declared default; without one, generation stops with an error, because the value is
     * built eagerly and a throwing argument would fail as soon as the no-op is first used.
     */
    private fun constructorCall(target: KSClassDeclaration): CodeBlock? {
        val arguments = mutableListOf<CodeBlock>()
        for (parameter in target.primaryConstructor!!.parameters) {
            if (parameter.isVararg) continue
            val name = parameter.name!!.asString()
            val type = parameter.type.resolve()
            val value = defaultValue(type)
            when {
                value != null -> arguments += CodeBlock.of("%N·=·%L", name, value)
                parameter.hasDefault -> Unit
                else -> {
                    logger.error(
                        "@NoOp has no default for parameter '$name' of type " +
                            "${type.declaration.simpleName.asString()}. Give the parameter a default value.",
                        parameter
                    )
                    return null
                }
            }
        }
        return CodeBlock.of("%T(%L)", target.toClassName(), arguments.joinToCode(",·"))
    }

    /** Renders [type] as source, writing function types in `(A) -> B` form and keeping a top-level receiver. */
    private fun typeName(type: KSType, resolver: TypeParameterResolver): TypeName {
        val name = lambdaForm(type.toTypeName(resolver))
        if (name !is LambdaTypeName || !type.hasReceiver()) return name
        return LambdaTypeName.get(
            receiver = name.parameters.first().type,
            parameters = name.parameters.drop(1),
            returnType = name.returnType
        ).copy(nullable = name.isNullable, suspending = name.isSuspending)
    }

    private fun lambdaForm(name: TypeName): TypeName {
        if (name !is ParameterizedTypeName) return name
        val arguments = name.typeArguments.map(::lambdaForm)
        val rawName = name.rawType.canonicalName
        val isFunction = FUNCTION_TYPE.matches(rawName)
        val isSuspendFunction = SUSPEND_FUNCTION_TYPE.matches(rawName)
        if ((!isFunction && !isSuspendFunction) || arguments.any { it is WildcardTypeName }) {
            return name.rawType.parameterizedBy(arguments).copy(nullable = name.isNullable)
        }
        return LambdaTypeName.get(
            parameters = arguments.dropLast(1).toTypedArray(),
            returnType = arguments.last()
        ).copy(nullable = name.isNullable, suspending = isSuspendFunction)
    }

    private fun KSType.hasReceiver(): Boolean =
        annotations.any { it.shortName.asString() == "ExtensionFunctionType" }

    private fun returnStatement(type: KSType, resolver: TypeParameterResolver): CodeBlock {
        val value = defaultValue(type, resolver)
        return if (value == null) {
            CodeBlock.of("%L\n", unsupported(type))
        } else {
            CodeBlock.of("return·%L\n", value)
        }
    }

    private fun unsupported(type: KSType): CodeBlock = CodeBlock.of(
        "throw·%T(%S)",
        UNSUPPORTED_OPERATION,
        "@NoOp has no default for ${type.declaration.simpleName.asString()}"
    )

    /** Returns the no-op value for [type], or null when there is no sensible one. */
    private fun defaultValue(
        type: KSType,
        resolver: TypeParameterResolver = TypeParameterResolver.EMPTY
    ): CodeBlock? {
        if (type.isMarkedNullable) return CodeBlock.of("null")

        val declaration = type.declaration
        if (declaration is KSTypeAlias) return defaultValue(declaration.type.resolve(), resolver)
        if (type.isFunctionType || type.isSuspendFunctionType) return emptyLambda(type, resolver)

        val qualifiedName = declaration.qualifiedName?.asString()
        if (qualifiedName == "kotlin.Array") return emptyArray(type)
        val default = DEFAULT_VALUES[qualifiedName] ?: return null
        if (type.arguments.any { it.variance == Variance.STAR }) {
            return explicitlyTypedFactory(default, type, resolver)
        }
        return CodeBlock.of(default)
    }

    /** `List<*>` gives the compiler nothing to infer from, so star arguments become `Any?`. */
    private fun explicitlyTypedFactory(
        default: String,
        type: KSType,
        resolver: TypeParameterResolver
    ): CodeBlock {
        val typeArguments = type.arguments.map { argument ->
            val name = argument.type?.takeIf { argument.variance != Variance.STAR }?.resolve()?.let { typeName(it, resolver) }
            CodeBlock.of("%T", name ?: ANY.copy(nullable = true))
        }
        return CodeBlock.of("%L<%L>()", default.removeSuffix("()"), typeArguments.joinToCode(",·"))
    }

    /** `emptyArray` is reified, so an array of a type parameter has no default. */
    private fun emptyArray(type: KSType): CodeBlock? {
        val elementType = type.arguments.single().type?.resolve() ?: return CodeBlock.of("kotlin.emptyArray<kotlin.Any?>()")
        return if (elementType.declaration is KSClassDeclaration || elementType.declaration is KSTypeAlias) {
            CodeBlock.of("kotlin.emptyArray()")
        } else {
            null
        }
    }

    /** Builds a lambda with the right parameter count; its body is the default of the return type. */
    private fun emptyLambda(functionType: KSType, resolver: TypeParameterResolver): CodeBlock {
        val returnType = functionType.arguments.last().type!!.resolve()
        val explicitParameters = functionType.arguments.size - 1 - if (functionType.hasReceiver()) 1 else 0
        val parameters = List(explicitParameters) { "_" }.joinToString(",·")
        val arrow = if (explicitParameters > 0) "$parameters·->·" else ""
        val isUnit = returnType.declaration.qualifiedName?.asString() == "kotlin.Unit" && !returnType.isMarkedNullable
        if (isUnit) return CodeBlock.of("{·$arrow}")
        return CodeBlock.of("{·$arrow%L·}", defaultValue(returnType, resolver) ?: unsupported(returnType))
    }

    /** Factories are fully qualified, so a member or parameter sharing a factory's name cannot capture the call. */
    private companion object {
        val UNSUPPORTED_OPERATION = ClassName("kotlin", "UnsupportedOperationException")
        val FUNCTION_TYPE = Regex("""kotlin\.Function\d+""")
        val SUSPEND_FUNCTION_TYPE = Regex("""kotlin\.coroutines\.SuspendFunction\d+""")

        val DEFAULT_VALUES = mapOf(
            "kotlin.Unit" to "kotlin.Unit",
            "kotlin.Boolean" to "false",
            "kotlin.Byte" to "0",
            "kotlin.Short" to "0",
            "kotlin.Int" to "0",
            "kotlin.Long" to "0L",
            "kotlin.Float" to "0f",
            "kotlin.Double" to "0.0",
            "kotlin.UByte" to "0u",
            "kotlin.UShort" to "0u",
            "kotlin.UInt" to "0u",
            "kotlin.ULong" to "0uL",
            "kotlin.Char" to "'\\u0000'",
            "kotlin.String" to "\"\"",
            "kotlin.BooleanArray" to "kotlin.booleanArrayOf()",
            "kotlin.ByteArray" to "kotlin.byteArrayOf()",
            "kotlin.ShortArray" to "kotlin.shortArrayOf()",
            "kotlin.IntArray" to "kotlin.intArrayOf()",
            "kotlin.LongArray" to "kotlin.longArrayOf()",
            "kotlin.FloatArray" to "kotlin.floatArrayOf()",
            "kotlin.DoubleArray" to "kotlin.doubleArrayOf()",
            "kotlin.CharArray" to "kotlin.charArrayOf()",
            "kotlin.collections.Iterable" to "kotlin.collections.emptyList()",
            "kotlin.collections.Collection" to "kotlin.collections.emptyList()",
            "kotlin.collections.List" to "kotlin.collections.emptyList()",
            "kotlin.collections.Set" to "kotlin.collections.emptySet()",
            "kotlin.collections.Map" to "kotlin.collections.emptyMap()",
            "kotlin.collections.MutableIterable" to "kotlin.collections.mutableListOf()",
            "kotlin.collections.MutableCollection" to "kotlin.collections.mutableListOf()",
            "kotlin.collections.MutableList" to "kotlin.collections.mutableListOf()",
            "kotlin.collections.MutableSet" to "kotlin.collections.mutableSetOf()",
            "kotlin.collections.MutableMap" to "kotlin.collections.mutableMapOf()",
            "kotlin.sequences.Sequence" to "kotlin.sequences.emptySequence()",
        )
    }
}
