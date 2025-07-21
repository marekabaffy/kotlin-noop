package com.marekabaffy.kotlinnoop.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

/**
 * KSP processor that generates no-operation implementations for @NoOp annotated classes.
 */
class NoOpAnnotationProcessor(
    environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val ignoredMethods = setOf("equals", "hashCode", "toString")

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("com.marekabaffy.kotlinnoop.annotations.NoOp")
        val unableToProcess = symbols.filterNot { it.validate() }.toMutableList()

        symbols
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDeclaration ->
                when {
                    classDeclaration.classKind == ClassKind.INTERFACE ->
                        generateNoOpForInterface(classDeclaration)

                    classDeclaration.classKind == ClassKind.CLASS ->
                        generateNoOpForClass(classDeclaration)

                    classDeclaration.isCompanionObject ->
                        generateCompanionExtension(classDeclaration)

                    else -> {
                        logger.error("@NoOp is not applicable to this symbol type.", classDeclaration)
                        unableToProcess.add(classDeclaration)
                    }
                }
            }

        return unableToProcess
    }

    /** Generates companion extension based on parent class type. */
    private fun generateCompanionExtension(companion: KSClassDeclaration) {
        val parent = companion.parentDeclaration as? KSClassDeclaration ?: return

        when (parent.classKind) {
            ClassKind.INTERFACE -> generateCompanionExtensionForInterface(parent)
            ClassKind.CLASS -> generateCompanionExtensionForClass(parent)
            else -> logger.error("@NoOp on a companion object is only supported for interfaces and classes.", parent)
        }
    }

    /** Generates object implementation for interface. */
    private fun generateNoOpForInterface(interfaceDeclaration: KSClassDeclaration) {
        val packageName = interfaceDeclaration.packageName.asString()
        val interfaceName = interfaceDeclaration.simpleName.asString()
        val generatedObjectName = "NoOp${interfaceName}"
        val visibility = getVisibilityModifier(interfaceDeclaration)

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(true, interfaceDeclaration.containingFile!!),
            packageName = packageName,
            fileName = generatedObjectName
        )
        OutputStreamWriter(file).use { writer ->
            writer.write("@file:Suppress(\"unused\")\n\n")
            writer.write("package $packageName\n\n")
            writer.write("${visibility}object $generatedObjectName : $interfaceName {\n\n")

            interfaceDeclaration.getAllProperties().forEach {
                writer.write(generateProperty(it))
            }
            interfaceDeclaration.getAllFunctions()
                .filter { it.simpleName.asString() !in ignoredMethods }
                .forEach {
                    writer.write(generateFunction(it))
                }
            writer.write("}\n")
        }
    }

    /** Generates val instance for class with constructor defaults. */
    private fun generateNoOpForClass(classDeclaration: KSClassDeclaration) {
        val constructor = classDeclaration.primaryConstructor
        if (constructor == null) {
            logger.error("@NoOp on a class requires a primary constructor.", classDeclaration)
            return
        }
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val generatedValName = "NoOp${className}"
        val visibility = getVisibilityModifier(classDeclaration)

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(true, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = generatedValName
        )
        OutputStreamWriter(file).use { writer ->
            writer.write("@file:Suppress(\"unused\")\n\n")
            writer.write("package $packageName\n\n")
            val constructorArgs = generateConstructorCallArgs(constructor)
            writer.write("${visibility}val $generatedValName = $className(\n")
            writer.write("$constructorArgs\n")
            writer.write(")\n")
        }
    }

    /** Generates Companion.NoOp extension for interface. */
    private fun generateCompanionExtensionForInterface(parentInterface: KSClassDeclaration) {
        val packageName = parentInterface.packageName.asString()
        val interfaceName = parentInterface.simpleName.asString()
        val visibility = getVisibilityModifier(parentInterface)

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(true, parentInterface.containingFile!!),
            packageName = packageName,
            fileName = "${interfaceName}CompanionNoOp"
        )
        OutputStreamWriter(file).use { writer ->
            writer.write("@file:Suppress(\"unused\")\n\n")
            writer.write("package $packageName\n\n")
            writer.write("${visibility}val $interfaceName.Companion.NoOp: $interfaceName\n")
            writer.write("    get() = object : $interfaceName {\n")
            parentInterface.getAllProperties().forEach {
                writer.write(generateProperty(it, "        "))
            }
            parentInterface.getAllFunctions()
                .filter { it.simpleName.asString() !in ignoredMethods }
                .forEach {
                    writer.write(generateFunction(it, "        "))
                }
            writer.write("    }\n")
        }
    }

    /** Generates Companion.NoOp extension for class. */
    private fun generateCompanionExtensionForClass(parentClass: KSClassDeclaration) {
        val constructor = parentClass.primaryConstructor
        if (constructor == null) {
            logger.error(
                "@NoOp on a companion object requires the containing class to have a primary constructor.",
                parentClass
            )
            return
        }

        val packageName = parentClass.packageName.asString()
        val className = parentClass.simpleName.asString()
        val visibility = getVisibilityModifier(parentClass)

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(true, parentClass.containingFile!!),
            packageName = packageName,
            fileName = "${className}CompanionNoOp"
        )

        OutputStreamWriter(file).use { writer ->
            writer.write("@file:Suppress(\"unused\")\n\n")
            writer.write("package $packageName\n\n")
            val constructorArgs = generateConstructorCallArgs(constructor)
            writer.write("${visibility}val $className.Companion.NoOp: $className\n")
            writer.write("    get() = $className(\n")
            writer.write("$constructorArgs\n")
            writer.write("    )\n")
        }
    }

    /** Returns visibility modifier keyword or empty string for public. */
    private fun getVisibilityModifier(declaration: KSDeclaration): String {
        val visibility = declaration.modifiers.firstOrNull {
            it in setOf(Modifier.PUBLIC, Modifier.INTERNAL, Modifier.PRIVATE, Modifier.PROTECTED)
        }
        return when (visibility) {
            Modifier.INTERNAL, Modifier.PRIVATE, Modifier.PROTECTED -> "${visibility.name.lowercase()} "
            else -> "" // Public is the default and can be omitted.
        }
    }

    /** Generates constructor call arguments with default values. */
    private fun generateConstructorCallArgs(constructor: KSFunctionDeclaration): String {
        return constructor.parameters.joinToString(separator = ",\n") { param ->
            val paramType = param.type.resolve()
            val defaultValue = if (paramType.isFunctionType || paramType.isSuspendFunctionType) {
                generateEmptyLambda(paramType)
            } else {
                getDefaultValueFor(paramType)
            }
            "    ${param.name!!.asString()} = $defaultValue"
        }
    }

    /** Generates property override with default value. */
    private fun generateProperty(property: KSPropertyDeclaration, indentation: String = "    "): String {
        val name = property.simpleName.asString()
        val type = property.type.toString()
        val defaultValue = getDefaultValueFor(property.type.resolve())

        val propertyString = if (property.isMutable) {
            """
            |override var $name: $type
            |${indentation}    get() = $defaultValue
            |${indentation}    set(value) {}
            """.trimMargin("|")
        } else {
            "override val $name: $type = $defaultValue"
        }
        return "$indentation$propertyString\n\n"
    }

    /** Generates function override with default return value. */
    private fun generateFunction(function: KSFunctionDeclaration, indentation: String = "    "): String {
        val name = function.simpleName.asString()
        val returnType = function.returnType!!.resolve()
        val parameters = function.parameters.joinToString(", ") { param ->
            "${param.name!!.asString()}: ${param.type}"
        }
        val suspendModifier = if (function.modifiers.contains(Modifier.SUSPEND)) "suspend " else ""
        val body = if (returnType.declaration.qualifiedName?.asString() == "kotlin.Unit") {
            "{}"
        } else {
            "{ return ${getDefaultValueFor(returnType)} }"
        }
        val signature = "override ${suspendModifier}fun $name($parameters): ${function.returnType}"
        return "$indentation$signature $body\n\n"
    }

    /** Returns appropriate default value for the given type. */
    private fun getDefaultValueFor(type: KSType): String {
        if (type.isMarkedNullable) return "null"
        return when (type.declaration.qualifiedName?.asString()) {
            "kotlin.Boolean" -> "false"
            "kotlin.Int" -> "0"
            "kotlin.Short" -> "0.toShort()"
            "kotlin.Long" -> "0L"
            "kotlin.Float" -> "0.0f"
            "kotlin.Double" -> "0.0"
            "kotlin.Char" -> "'\\u0000'"
            "kotlin.String" -> "\"\""
            "kotlin.collections.List", "kotlin.collections.MutableList" -> "emptyList()"
            "kotlin.collections.Set", "kotlin.collections.MutableSet" -> "emptySet()"
            "kotlin.collections.Map", "kotlin.collections.MutableMap" -> "emptyMap()"
            else -> "throw UnsupportedOperationException(\"Not implemented\")"
        }
    }

    /** Generates empty lambda with correct parameter count and return type. */
    private fun generateEmptyLambda(functionType: KSType): String {
        val paramCount = functionType.arguments.size - 1 // Last argument is return type
        val returnType = functionType.arguments.lastOrNull()?.type?.resolve()
        
        if (paramCount <= 0) {
            return if (returnType?.declaration?.qualifiedName?.asString() == "kotlin.Unit") {
                "{}"
            } else {
                "{ ${getDefaultValueFor(returnType!!)} }"
            }
        }
        
        val underscores = List(paramCount) { "_" }.joinToString(", ")
        return if (returnType?.declaration?.qualifiedName?.asString() == "kotlin.Unit") {
            "{ $underscores -> }"
        } else {
            "{ $underscores -> ${getDefaultValueFor(returnType!!)} }"
        }
    }
}