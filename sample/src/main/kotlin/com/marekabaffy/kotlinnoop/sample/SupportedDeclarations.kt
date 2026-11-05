package com.marekabaffy.kotlinnoop.sample

import com.marekabaffy.kotlinnoop.annotations.NoOp
import java.util.UUID

typealias Names = List<String>

@NoOp
interface Scalars {
    val booleanValue: Boolean
    val byteValue: Byte
    val shortValue: Short
    val intValue: Int
    val longValue: Long
    val floatValue: Float
    val doubleValue: Double
    val charValue: Char
    val stringValue: String
    val ubyteValue: UByte
    val ushortValue: UShort
    val uintValue: UInt
    val ulongValue: ULong
    val nullableValue: String?
    var mutableValue: Int
}

@NoOp
interface Arrays {
    fun booleans(): BooleanArray
    fun bytes(): ByteArray
    fun shorts(): ShortArray
    fun ints(): IntArray
    fun longs(): LongArray
    fun floats(): FloatArray
    fun doubles(): DoubleArray
    fun chars(): CharArray
    fun strings(): Array<String>
    fun projected(): Array<out CharSequence>
    fun starred(): Array<*>
}

@NoOp
interface Collections {
    fun iterable(): Iterable<Int>
    fun collection(): Collection<Int>
    fun list(): List<Int>
    fun set(): Set<Int>
    fun map(): Map<String, Int>
    fun sequence(): Sequence<Int>
    fun mutableIterable(): MutableIterable<Int>
    fun mutableCollection(): MutableCollection<Int>
    fun mutableList(): MutableList<Int>
    fun mutableSet(): MutableSet<Int>
    fun mutableMap(): MutableMap<String, Int>
    fun nested(): Map<String, List<Set<Int>>>
    fun starred(): List<*>
    fun mutableStarred(): MutableList<*>
    fun aliased(): Names
}

@NoOp
interface FunctionTypes {
    fun returnsUnitLambda(): () -> Unit
    fun returnsIntLambda(): () -> Int
    fun returnsTwoArgumentLambda(): (String, Int) -> String
    fun returnsSuspendLambda(): suspend (Int) -> String
    fun returnsReceiverLambda(): String.() -> Int
    fun returnsCustomLambda(): () -> Sample
    fun takesLambdas(first: (Int) -> Unit, second: suspend () -> Int, third: String.(Int) -> Unit)
}

@NoOp
interface Signatures {
    fun returnsCustom(): Sample
    val customProperty: Sample
    fun nothing(): Nothing
    fun nullableCustom(): Sample?
    fun takesVararg(vararg values: String): Int
    fun takesGenericVararg(vararg values: List<Int>)
    fun takesOtherPackage(id: UUID): UUID?
    fun takesGeneric(values: Map<String, List<Int>>): Int
    fun <T> generic(value: T): T?
    fun <T> genericThrowing(value: T): T
    fun <T : Comparable<T>> bounded(values: List<T>): List<T>
    fun <T> genericArray(): Array<T>
    fun String.extensionFunction(): Int
    val String.extensionProperty: Int
    suspend fun suspending(value: Int): Int
    fun toString(radix: Int): String
    fun equals(value: String?): Boolean
    fun returnsNullableUnitLambda(): () -> Unit?
    fun hasDefaultImplementation(): Int = 42
}

@NoOp
interface RedeclaresAny {
    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

interface GenericParent<T> {
    val inherited: T?
    fun echo(value: T): List<T>
}

@NoOp
interface InheritsGeneric : GenericParent<String>

class Outer {
    @NoOp
    interface NestedInterface {
        fun value(): Int
    }

    interface NestedWithCompanion {
        fun value(): Int

        @NoOp
        companion object
    }

    @NoOp
    data class NestedClass(val value: Int)
}

@NoOp
internal interface InternalInterface {
    fun value(): Int
}

interface NamedCompanion {
    fun value(): Int

    @NoOp
    companion object Factory
}

@NoOp
data class ClassParameters(
    val byteValue: Byte,
    val strings: Array<String>,
    val mutableList: MutableList<Int>,
    val map: Map<String, List<Int>>,
    val nullableLambda: (() -> Unit)?,
    val receiverLambda: String.(Int) -> Int,
    val keepsOwnDefault: Sample = Sample(7),
    val replacesOwnDefault: Int = 5,
)

@NoOp
class VarargClass(val first: Int, vararg val rest: String)
