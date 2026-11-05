package com.marekabaffy.kotlinnoop.processor

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.useKsp2
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

@OptIn(ExperimentalCompilerApi::class)
class NoOpAnnotationProcessorTest {

    /** Each source must generate code that compiles against the declaration it came from. */
    @TestFactory
    fun supportedDeclarationsCompile(): List<DynamicTest> = listOf(
        "generic return" to "@NoOp interface A { fun f(): List<Int> }",
        "generic parameter" to "@NoOp interface A { fun f(x: Map<String, List<Int>>): Int }",
        "nullable return" to "@NoOp interface A { fun f(): String?; val p: Int? }",
        "nullable unit return" to "@NoOp interface A { fun f(): Unit? }",
        "function return" to "@NoOp interface A { fun f(): () -> Int }",
        "suspend receiver lambda" to "@NoOp interface A { fun f(): suspend String.(Int) -> Int }",
        "nullable receiver lambda" to "@NoOp interface A { fun f(): (String.() -> Unit)? }",
        "nested lambdas" to """
            @NoOp interface A {
                fun f(): List<() -> Unit>
                fun g(x: List<suspend () -> Unit>)
                fun h(): Map<String, String.() -> Int>
            }
        """,
        "lambda returning lambda" to "@NoOp interface A { fun f(): (Int) -> (String) -> MutableList<*> }",
        "explicit function types" to "@NoOp interface A { fun f(): Function1<Int, Int>; fun g(): Function1<*, *>? }",
        "star projected collections" to """
            @NoOp interface A {
                fun f(): List<*>
                fun g(): Map<String, *>
                fun h(): MutableMap<String, *>
                fun i(): MutableMap<*, *>
                fun <T> j(): MutableMap<T, *>
            }
        """,
        "variance" to "@NoOp interface A { fun f(): MutableList<out CharSequence>; fun g(): MutableList<in String> }",
        "arrays" to """
            @NoOp interface A {
                fun f(): Array<List<Int>>
                fun <T> g(): Array<List<T>>
                fun h(): Array<Array<Int>>
                fun i(): Array<*>
                fun <T> j(): Array<T>
            }
        """,
        "type aliases" to """
            typealias Callback<T> = (T) -> Unit
            typealias Text = String
            typealias MaybeText = String?
            typealias Nothingness = Unit
            typealias Texts = Array<Text>
            @NoOp interface A {
                fun f(): Callback<Int>
                fun g(x: Callback<String>)
                fun h(): Text
                fun i(): MaybeText
                fun j(): Nothingness
                fun k(): () -> Nothingness
                fun l(): Texts
            }
        """,
        "vararg" to "@NoOp interface A { fun f(vararg x: String); fun g(first: Int, vararg rest: List<Int>): Int }",
        "other package" to "@NoOp interface A { fun f(): java.io.File?; fun g(x: java.util.UUID) }",
        "generic function" to "@NoOp interface A { fun <T> f(x: T): T?; fun <T> g(x: T): T }",
        "where bounds" to "@NoOp interface A { fun <T> f(x: T): Int where T : Comparable<T>, T : CharSequence }",
        "extension members" to """
            @NoOp interface A {
                fun String.f(): Int
                val String.p: Int
                var String.q: Int
                fun (() -> Unit).g()
                val <T> List<T>.r: Int
            }
        """,
        "mutable property" to "@NoOp interface A { var p: Int; var q: List<Int>?; var r: () -> Unit }",
        "suspend function" to "@NoOp interface A { suspend fun f(x: Int): List<Int> }",
        "keyword names" to "@NoOp interface A { fun `in`(`object`: Int): Int; val `fun`: Int }",
        "operator and infix" to """
            @NoOp interface A {
                operator fun plus(x: Int): A?
                infix fun to(x: Int): Int
                operator fun invoke(): Int
            }
        """,
        "private members" to "@NoOp interface A { fun f(); private fun g() = 1; private val p get() = 1 }",
        "default implementations" to "@NoOp interface A { fun f(): Int = 1; val p: Int get() = 1 }",
        "any member overloads" to "@NoOp interface A { fun toString(radix: Int): String; fun hashCode(x: Int): Int; fun equals(): Boolean }",
        "any member overrides" to "@NoOp interface A { override fun toString(): String; override fun equals(other: Any?): Boolean; override fun hashCode(): Int }",
        "equals overloads" to """
            @NoOp interface A {
                fun equals(): Boolean
                fun equals(x: String?): Boolean
                fun equals(x: Int, y: Int): Boolean
                fun String.equals(other: Any?): Boolean
            }
        """,
        "lambda returning nullable" to "@NoOp interface A { fun f(): () -> Unit?; fun g(): (Int) -> String? }",
        "context function type" to """
            @NoOp interface A {
                fun f(): context(String) () -> Int
                fun g(): context(String) (Int) -> Unit
                fun h(x: context(String) () -> Unit)
            }
        """,
        "context function type with receiver" to """
            @NoOp interface A {
                fun f(): context(String, Int) Long.(Boolean) -> Char
                fun g(): (context(String) () -> Unit)?
            }
        """,
        "parameters named like factories" to """
            @NoOp interface A {
                fun f(emptyList: Int): List<Int>
                fun g(mutableListOf: Int): MutableList<*>
                fun h(emptyArray: Int): Array<*>
                fun i(Unit: Int): () -> Unit
            }
        """,
        "same package declarations named like factories" to """
            fun emptyList(): Int = 1
            fun mutableMapOf(): Int = 1
            fun intArrayOf(): Int = 1
            @NoOp class A(val a: List<Int>, val b: MutableMap<String, Int>, val c: IntArray)
            @NoOp interface B { fun a(): List<Int>; fun b(): MutableMap<String, Int>; fun c(): IntArray }
        """,
        "same package types named like kotlin types" to """
            class List
            class String
            class Unit
            @NoOp interface A {
                fun f(): kotlin.collections.List<kotlin.Int>
                fun g(): kotlin.String
                fun h(): () -> kotlin.Unit
                fun i(x: List): String?
            }
        """,
        "member shadowing the Unit value" to """
            typealias Nothingness = Unit
            @NoOp interface A { val Unit: Int; fun f(): Nothingness }
        """,
        "interface and its companion both annotated" to "@NoOp interface A { fun f(); @NoOp companion object }",
        "internal constructor" to "@NoOp class A internal constructor(val x: Int)",
        "fun interface" to "@NoOp fun interface A { fun f(x: Int): Int }",
        "sealed interface" to "@NoOp sealed interface A { fun f() }",
        "java and generic supertypes" to "@NoOp interface A : Runnable, Comparable<A>",
        "inherited generic members" to """
            interface Parent<T> { val p: T?; fun f(x: T): List<T>; fun g(): T }
            @NoOp interface A : Parent<String>
        """,
        "empty interface" to "@NoOp interface A",
        "interface companion" to "interface A { fun f(): List<Int>; @NoOp companion object }",
        "named companion" to "interface A { fun f(): Int; @NoOp companion object Factory }",
        "class companion" to "data class A(val x: Int) { @NoOp companion object }",
        "nested interface" to "class O { @NoOp interface A { fun f() } }",
        "nested class" to "class O { @NoOp data class A(val x: Int) }",
        "deeply nested companion" to "class O { class P { interface A { fun f(); @NoOp companion object } } }",
        "same nested names" to """
            class O { @NoOp interface A { fun f() } }
            class P { @NoOp interface A { fun f() } }
        """,
        "internal" to "@NoOp internal interface A { fun f() }",
        "class without parameters" to "@NoOp class A",
        "class parameters" to """
            @NoOp class A(
                val a: Byte,
                val b: MutableList<Int>,
                val c: IntArray,
                val d: (() -> Unit)?,
                val e: suspend (Int) -> String,
                val f: String.(Int) -> Int,
                val g: MutableMap<*, *>,
            )
        """,
        "class parameter with own default" to """
            data class P(val v: Int)
            @NoOp class A(val p: P = P(1), val l: (P) -> P)
        """,
        "class vararg parameter" to "@NoOp class A(val x: Int, vararg val rest: String)",
        "class keyword parameter" to "@NoOp class A(val `in`: Int)",
    ).map { (name, source) ->
        dynamicTest(name) {
            val outcome = compile(source)
            assertEquals(ExitCode.OK, outcome.exitCode, outcome.messages)
        }
    }

    /** Each source must fail generation with the message, not with invalid generated code. */
    @TestFactory
    fun unsupportedDeclarationsAreRejected(): List<DynamicTest> {
        val notApplicable = "@NoOp is only applicable to interfaces, classes and their companion objects."
        val generic = "@NoOp does not support generic declarations"
        val inaccessible = "@NoOp does not support private or protected declarations"
        val notInstantiable = "@NoOp does not support abstract or sealed classes"
        return listOf(
            Triple("enum", "@NoOp enum class A { X }", notApplicable),
            Triple("object", "@NoOp object A", notApplicable),
            Triple("data object", "@NoOp data object A", notApplicable),
            Triple("annotation class", "@NoOp annotation class A", notApplicable),
            Triple("enum companion", "enum class A { X; @NoOp companion object }", notApplicable),
            Triple("generic interface", "@NoOp interface A<T> { fun f(x: T): Int }", generic),
            Triple("generic class", "@NoOp class A<T>(val x: List<T>)", generic),
            Triple("generic interface companion", "interface A<T> { @NoOp companion object }", generic),
            Triple("generic class companion", "class A<T>(val x: T) { @NoOp companion object }", generic),
            Triple("private interface", "@NoOp private interface A { fun f() }", inaccessible),
            Triple("private class", "@NoOp private class A", inaccessible),
            Triple("inside private", "class O { private interface I { @NoOp interface A } }", inaccessible),
            Triple("protected interface", "abstract class O { @NoOp protected interface A }", inaccessible),
            Triple("private companion", "class A private constructor() { @NoOp private companion object }", "@NoOp does not support classes with a private or protected primary constructor."),
            Triple("abstract class", "@NoOp abstract class A(val x: Int)", notInstantiable),
            Triple("sealed class", "@NoOp sealed class A(val x: Int)", notInstantiable),
            Triple("abstract class companion", "abstract class A { @NoOp companion object }", notInstantiable),
            Triple("inner class", "class O { @NoOp inner class A(val x: Int) }", "@NoOp does not support inner classes"),
            Triple("private constructor", "@NoOp class A private constructor(val x: Int)", "@NoOp does not support classes with a private or protected primary constructor."),
            Triple("protected constructor", "@NoOp open class A protected constructor(val x: Int)", "@NoOp does not support classes with a private or protected primary constructor."),
            Triple("protected constructor companion", "open class A protected constructor(val x: Int) { @NoOp companion object }", "@NoOp does not support classes with a private or protected primary constructor."),
            Triple("sealed interface companion", "sealed interface A { @NoOp companion object }", "@NoOp does not support companion objects of sealed interfaces"),
            Triple("no primary constructor", "@NoOp class A { constructor(x: Int) }", "@NoOp on a class requires a primary constructor."),
            Triple("companion without primary constructor", "class A { constructor(x: Int); @NoOp companion object }", "@NoOp on a class requires a primary constructor."),
            Triple("parameter without a default", "class P\n@NoOp class A(val p: P)", "@NoOp has no default for parameter 'p' of type P."),
            Triple("companion parameter without a default", "class P\nclass A(val p: P) { @NoOp companion object }", "@NoOp has no default for parameter 'p' of type P."),
            Triple("type parameter without a default", "class P\nclass O { @NoOp class A(val a: Int, val p: P) }", "@NoOp has no default for parameter 'p' of type P."),
        ).map { (name, source, message) ->
            dynamicTest(name) {
                val outcome = compile(source)
                assertEquals(ExitCode.COMPILATION_ERROR, outcome.exitCode, outcome.messages)
                assertContains(outcome.messages, "[ksp]")
                assertContains(outcome.messages, message)
            }
        }
    }

    /**
     * KSP 2.3.13 declares `contextParameters` but its implementation always returns an empty list, so the
     * override cannot be generated. When this test fails after a KSP upgrade, generate them and update the README.
     */
    @TestFactory
    fun contextParametersAreALimitationOfKsp(): List<DynamicTest> = listOf(
        "function" to "@NoOp interface A { context(logger: String) fun f(x: Int): Int }",
        "property" to "@NoOp interface A { context(logger: String) val p: Int }",
        "inherited" to "interface Parent { context(logger: String) fun f(): Int }\n@NoOp interface A : Parent",
    ).map { (name, source) ->
        dynamicTest(name) {
            val outcome = compile(source)
            assertEquals(ExitCode.COMPILATION_ERROR, outcome.exitCode, outcome.messages)
            assertContains(outcome.messages, "overrides nothing")
        }
    }

    /** KSP 2.3.13 does not mark context function types, so the lambda gets one parameter too many. */
    @Test
    fun contextFunctionTypeAsClassParameterIsALimitationOfKsp() {
        val outcome = compile("@NoOp class A(val f: context(String) (Int) -> Int)")
        assertEquals(ExitCode.COMPILATION_ERROR, outcome.exitCode, outcome.messages)
        assertContains(outcome.messages, "Argument type mismatch")
    }

    @Test
    fun processorOptsInToNewFeatures() {
        val outcome = compile("@NoOp interface A")
        assertFalse("has not opted in" in outcome.messages, outcome.messages)
    }

    @TestFactory
    fun generatedNameCollisionsAreRejected(): List<DynamicTest> = listOf(
        Triple(
            "nested and top level",
            "class Outer { @NoOp interface Inner }\n@NoOp interface OuterInner",
            "@NoOp generates 'NoOpOuterInner' for both"
        ),
        Triple(
            "two nesting shapes",
            "class AB { @NoOp interface C }\nclass A { class BC; @NoOp interface BC2 }\nclass A2 { class B { @NoOp interface C } }\n@NoOp interface ABC",
            "@NoOp generates 'NoOpABC' for both"
        ),
        Triple(
            "companions",
            "class Outer { interface Inner { @NoOp companion object } }\ninterface OuterInner { @NoOp companion object }",
            "@NoOp generates 'OuterInnerCompanionNoOp' for both"
        ),
        Triple(
            "names differing only by case",
            "@NoOp interface Foo\n@NoOp interface FOO",
            "for both test.Foo and test.FOO"
        ),
    ).map { (name, source, message) ->
        dynamicTest(name) {
            val outcome = compile(source)
            assertEquals(ExitCode.COMPILATION_ERROR, outcome.exitCode, outcome.messages)
            assertContains(outcome.messages, message)
            assertFalse("FileAlreadyExists" in outcome.messages, outcome.messages)
        }
    }

    @Test
    fun sameNameInDifferentPackagesIsNotACollision() {
        val outcome = compile(
            sources = listOf(
                source("first/Source.kt", "first", "@NoOp interface A"),
                source("second/Source.kt", "second", "@NoOp interface A"),
            )
        )
        assertEquals(ExitCode.OK, outcome.exitCode, outcome.messages)
        assertEquals(listOf("NoOpA.kt", "NoOpA.kt"), outcome.generatedFileNames)
    }

    /** The fully qualified call has a known limit, which fails at compile time rather than at runtime. */
    @Test
    fun aMemberNamedKotlinIsALimitation() {
        val outcome = compile("@NoOp interface A { val kotlin: Int; fun f(): List<Int> }")
        assertEquals(ExitCode.COMPILATION_ERROR, outcome.exitCode, outcome.messages)
    }

    @Test
    fun factoryCallsAreFullyQualified() {
        val generated = compile(
            "@NoOp interface A { fun a(): List<Int>; fun b(): MutableSet<Int>; fun c(): Sequence<Int>; fun d(): IntArray; fun e(): Array<String>; fun f(): Array<*> }"
        ).generatedSources
        assertContains(generated, "kotlin.collections.emptyList()")
        assertContains(generated, "kotlin.collections.mutableSetOf()")
        assertContains(generated, "kotlin.sequences.emptySequence()")
        assertContains(generated, "kotlin.intArrayOf()")
        assertContains(generated, "kotlin.emptyArray()")
        assertContains(generated, "kotlin.emptyArray<kotlin.Any?>()")
    }

    @Test
    fun rejectionGeneratesNothing() {
        val outcome = compile("@NoOp interface A<T>")
        assertEquals(emptyList(), outcome.generatedFileNames)
    }

    @Test
    fun oneRejectionDoesNotHideAnother() {
        val outcome = compile("@NoOp enum class A { X }\n@NoOp interface B<T>")
        assertContains(outcome.messages, "@NoOp is only applicable")
        assertContains(outcome.messages, "@NoOp does not support generic declarations")
    }

    @Test
    fun unannotatedDeclarationsGenerateNothing() {
        val outcome = compile("interface A { fun f(): Int }")
        assertEquals(ExitCode.OK, outcome.exitCode, outcome.messages)
        assertEquals(emptyList(), outcome.generatedFileNames)
    }

    @Test
    fun generatedFileNames() {
        val outcome = compile(
            """
            @NoOp interface DirectInterface
            @NoOp class DirectClass
            interface CompanionInterface { @NoOp companion object }
            class CompanionClass { @NoOp companion object }
            class Outer {
                @NoOp interface Inner
                interface InnerWithCompanion { @NoOp companion object }
            }
            """
        )
        assertEquals(ExitCode.OK, outcome.exitCode, outcome.messages)
        assertEquals(
            listOf(
                "CompanionClassCompanionNoOp.kt",
                "CompanionInterfaceCompanionNoOp.kt",
                "NoOpDirectClass.kt",
                "NoOpDirectInterface.kt",
                "NoOpOuterInner.kt",
                "OuterInnerWithCompanionCompanionNoOp.kt",
            ),
            outcome.generatedFileNames
        )
    }

    /** Internal cannot be read back through reflection from Kotlin source, so these check the generated text. */
    @TestFactory
    fun visibilityFollowsTheDeclaration(): List<DynamicTest> = listOf(
        Triple("public interface", "@NoOp interface A", "public object NoOpA"),
        Triple("internal interface", "@NoOp internal interface A", "internal object NoOpA"),
        Triple("public class", "@NoOp class A", "public val NoOpA"),
        Triple("internal class", "@NoOp internal class A", "internal val NoOpA"),
        Triple("inside internal", "internal class O { @NoOp interface A }", "internal object NoOpOA"),
        Triple("internal companion", "internal interface A { @NoOp companion object }", "internal val A.Companion.NoOp"),
    ).map { (name, source, expected) ->
        dynamicTest(name) {
            val outcome = compile(source)
            assertEquals(ExitCode.OK, outcome.exitCode, outcome.messages)
            assertContains(outcome.generatedSources, expected)
        }
    }

    @Test
    fun functionTypesAreWrittenInSourceForm() {
        val generated = compile(
            "@NoOp interface A { fun f(): suspend (Int) -> String; fun g(): String.(Int) -> Int; fun h(): List<() -> Unit> }"
        ).generatedSources
        assertContains(generated, "suspend (Int) -> String")
        assertContains(generated, "String.(Int) -> Int")
        assertContains(generated, "List<() -> Unit>")
        assertFalse("SuspendFunction" in generated, generated)
        assertFalse("Function0" in generated, generated)
    }

    @Test
    fun generatedCodeStaysClean() {
        val generated = compile(
            "@NoOp interface A { fun f(); fun g(): () -> Unit; fun h(): (Int) -> Unit }"
        ).generatedSources
        assertContains(generated, "@file:Suppress(\"unused\")")
        assertFalse("Unit }" in generated.replace("-> Unit", ""), generated)
        assertFalse("return kotlin.Unit" in generated, generated)
        assertFalse("= kotlin.Unit" in generated, generated)
    }

    @Test
    fun declarationWaitsForATypeGeneratedInAnEarlierRound() {
        val outcome = compile(
            "@NoOp interface A { fun f(): test.Generated? }",
            GeneratedTypeProvider()
        )
        assertEquals(ExitCode.OK, outcome.exitCode, outcome.messages)
        assertEquals(listOf("Generated.kt", "NoOpA.kt"), outcome.generatedFileNames)
    }

    private class Outcome(val exitCode: ExitCode, val messages: String, private val workingDir: File) {
        private val generatedFiles: List<File>
            get() = workingDir.walkTopDown()
                .filter { it.extension == "kt" && it.name != SOURCE_FILE_NAME && it.name != "Source.kt" }
                .sortedBy { it.name }
                .toList()

        val generatedFileNames: List<String> get() = generatedFiles.map { it.name }
        val generatedSources: String get() = generatedFiles.joinToString("\n") { it.readText() }
    }

    private fun source(fileName: String, packageName: String, body: String) = SourceFile.kotlin(
        fileName,
        "package $packageName\n\nimport com.marekabaffy.kotlinnoop.annotations.NoOp\n\n${body.trimIndent()}\n"
    )

    private fun compile(source: String, vararg extraProviders: SymbolProcessorProvider): Outcome =
        compile(listOf(source(SOURCE_FILE_NAME, "test", source)), *extraProviders)

    private fun compile(sources: List<SourceFile>, vararg extraProviders: SymbolProcessorProvider): Outcome {
        val compilation = KotlinCompilation().apply {
            this.sources = sources
            inheritClassPath = true
            messageOutputStream = ByteArrayOutputStream()
            useKsp2()
            configureKsp {
                withCompilation = true
                symbolProcessorProviders += NoOpAnnotationProcessorProvider()
                symbolProcessorProviders += extraProviders
            }
        }
        val result = compilation.compile()
        return Outcome(result.exitCode, result.messages, compilation.workingDir)
    }

    /** Generates `test.Generated` in the first round, so a declaration using it cannot validate until the second. */
    private class GeneratedTypeProvider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment) = object : SymbolProcessor {
            private var generated = false

            override fun process(resolver: Resolver): List<KSAnnotated> {
                if (!generated) {
                    generated = true
                    environment.codeGenerator
                        .createNewFile(Dependencies(false), "test", "Generated")
                        .use { it.write("package test\n\nclass Generated\n".toByteArray()) }
                }
                return emptyList()
            }
        }
    }

    private companion object {
        const val SOURCE_FILE_NAME = "TestSource.kt"
    }
}
