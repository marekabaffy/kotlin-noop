package com.marekabaffy.kotlinnoop.sample

import com.marekabaffy.kotlinnoop.annotations.NoOp

data class Sample(
    val value: Int
)

@NoOp
interface SampleInterface {
    fun sampleMethod0()
    fun sampleMethod1(arg1: Unit): Boolean
    suspend fun sampleMethod2(arg1: Unit, arg2: Unit): Sample
}

interface SampleCompanionInterface {
    fun sampleMethod0()
    fun sampleMethod1(arg1: Unit): Boolean
    suspend fun sampleMethod2(arg1: Unit, arg2: Unit): Sample

    @NoOp
    companion object
}

@NoOp
data class SampleClass(
    val sampleMethod: () -> Unit,
    val sampleMethod1: (arg1: Unit) -> Boolean,
    val sampleMethod2: suspend (arg1: Unit, arg2: Unit) -> Sample
)

data class SampleCompanionClass(
    val sampleMethod: () -> Unit,
    val sampleMethod1: (arg1: Unit) -> Boolean,
    val sampleMethod2: suspend (arg1: Unit, arg2: Unit) -> Sample
) {
    @NoOp
    companion object
}

fun main() {
    val sampleInterface: SampleInterface = NoOpSampleInterface

    val sampleCompanionInterface: SampleCompanionInterface = SampleCompanionInterface.NoOp

    val sampleClass: SampleClass = NoOpSampleClass

    val sampleCompanionClass: SampleCompanionClass = SampleCompanionClass.NoOp
}