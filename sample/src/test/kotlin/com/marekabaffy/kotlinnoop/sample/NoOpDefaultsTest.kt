package com.marekabaffy.kotlinnoop.sample

import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NoOpDefaultsTest {

    @Test
    fun scalarsHaveZeroValues() = with(NoOpScalars) {
        assertEquals(false, booleanValue)
        assertEquals(0.toByte(), byteValue)
        assertEquals(0.toShort(), shortValue)
        assertEquals(0, intValue)
        assertEquals(0L, longValue)
        assertEquals(0f, floatValue)
        assertEquals(0.0, doubleValue)
        assertEquals('\u0000', charValue)
        assertEquals("", stringValue)
        assertEquals(0.toUByte(), ubyteValue)
        assertEquals(0.toUShort(), ushortValue)
        assertEquals(0u, uintValue)
        assertEquals(0uL, ulongValue)
        assertNull(nullableValue)
    }

    @Test
    fun settingAPropertyIsIgnored() {
        NoOpScalars.mutableValue = 3
        assertEquals(0, NoOpScalars.mutableValue)
    }

    @Test
    fun arraysAreEmpty() = with(NoOpArrays) {
        assertEquals(0, booleans().size)
        assertEquals(0, bytes().size)
        assertEquals(0, shorts().size)
        assertEquals(0, ints().size)
        assertEquals(0, longs().size)
        assertEquals(0, floats().size)
        assertEquals(0, doubles().size)
        assertEquals(0, chars().size)
        assertEquals(0, strings().size)
        assertEquals(0, projected().size)
        assertEquals(0, starred().size)
    }

    @Test
    fun collectionsAreEmpty() = with(NoOpCollections) {
        assertEquals(emptyList(), iterable().toList())
        assertEquals(emptyList(), collection().toList())
        assertEquals(emptyList(), list())
        assertEquals(emptySet(), set())
        assertEquals(emptyMap(), map())
        assertEquals(emptyList(), sequence().toList())
        assertEquals(emptyMap(), nested())
        assertEquals(emptyList<Any?>(), starred())
        assertEquals(emptyList<Any?>(), mutableStarred())
        assertEquals(emptyList(), aliased())
    }

    @Test
    fun mutableCollectionsAcceptWrites() = with(NoOpCollections) {
        assertEquals(emptyList(), mutableIterable().toList())
        assertTrue(mutableCollection().add(1))
        assertTrue(mutableList().add(1))
        assertTrue(mutableSet().add(1))
        assertNull(mutableMap().put("key", 1))
    }

    @Test
    fun lambdasReturnDefaults() = with(NoOpFunctionTypes) {
        assertEquals(Unit, returnsUnitLambda()())
        assertEquals(0, returnsIntLambda()())
        assertEquals("", returnsTwoArgumentLambda()("a", 1))
        assertEquals("", runSuspending { returnsSuspendLambda()(1) })
        assertEquals(0, "receiver".(returnsReceiverLambda())())
        assertFailsWith<UnsupportedOperationException> { returnsCustomLambda()() }
        takesLambdas({}, { 1 }, {})
    }

    @Test
    fun typesWithoutADefaultThrowOnlyWhenUsed() {
        val noOp: Signatures = NoOpSignatures
        val custom = assertFailsWith<UnsupportedOperationException> { noOp.returnsCustom() }
        assertEquals("@NoOp has no default for Sample", custom.message)
        assertFailsWith<UnsupportedOperationException> { noOp.customProperty }
        assertFailsWith<UnsupportedOperationException> { noOp.nothing() }
        assertFailsWith<UnsupportedOperationException> { noOp.genericThrowing("value") }
        assertFailsWith<UnsupportedOperationException> { noOp.genericArray<String>() }
    }

    @Test
    fun signaturesAreOverridden() = with(NoOpSignatures) {
        assertNull(nullableCustom())
        assertEquals(0, takesVararg("a", "b"))
        takesGenericVararg(listOf(1))
        assertNull(takesOtherPackage(UUID.randomUUID()))
        assertEquals(0, takesGeneric(emptyMap()))
        assertNull(generic("value"))
        assertEquals(emptyList(), bounded(listOf(1)))
        assertEquals(0, "receiver".extensionFunction())
        assertEquals(0, "receiver".extensionProperty)
        assertEquals(0, runSuspending { suspending(1) })
        assertEquals("", toString(16))
        assertFalse(equals(value = null))
        assertNull(returnsNullableUnitLambda()())
    }

    @Test
    fun defaultImplementationsAreReplaced() {
        assertEquals(0, NoOpSignatures.hasDefaultImplementation())
    }

    @Test
    fun anyMembersKeepTheirBehaviour() {
        assertTrue(NoOpSignatures.toString().contains("NoOpSignatures"))
        assertEquals(NoOpSignatures, NoOpSignatures)
    }

    @Test
    fun membersNamedLikeFactoriesDoNotCallThemselves() {
        val noOp: MembersNamedLikeFactories = NoOpMembersNamedLikeFactories
        assertTrue(noOp.emptyList().isEmpty())
        assertTrue(noOp.emptySet().isEmpty())
        assertTrue(noOp.emptyMap().isEmpty())
        assertEquals(0, noOp.emptySequence().count())
        assertEquals(0, noOp.emptyArray().size)
        assertEquals(0, noOp.intArrayOf().size)
        assertTrue(noOp.mutableListOf().isEmpty())
        assertTrue(noOp.mutableSetOf().isEmpty())
        assertTrue(noOp.mutableMapOf().isEmpty())
    }

    @Test
    fun redeclaredAnyMembersGetDefaults() {
        val noOp: RedeclaresAny = NoOpRedeclaresAny
        assertEquals("", noOp.toString())
        assertEquals(0, noOp.hashCode())
        assertEquals(noOp, noOp)
        assertFalse(noOp.equals("other"))
    }

    @Test
    fun inheritedGenericMembersUseTheConcreteType() {
        val noOp: InheritsGeneric = NoOpInheritsGeneric
        assertNull(noOp.inherited)
        assertEquals(emptyList(), noOp.echo("value"))
    }

    @Test
    fun nestedDeclarationsAreGenerated() {
        assertEquals(0, NoOpOuterNestedInterface.value())
        assertEquals(0, Outer.NestedWithCompanion.NoOp.value())
        assertEquals(Outer.NestedClass(0), NoOpOuterNestedClass)
    }

    @Test
    fun internalInterfaceIsGenerated() {
        assertEquals(0, NoOpInternalInterface.value())
    }

    @Test
    fun namedCompanionIsTheReceiver() {
        assertEquals(0, NamedCompanion.NoOp.value())
    }

    @Test
    fun classParametersGetDefaults() = with(NoOpClassParameters) {
        assertEquals(0.toByte(), byteValue)
        assertEquals(0, strings.size)
        assertEquals(emptyList(), mutableList)
        assertEquals(emptyMap(), map)
        assertNull(nullableLambda)
        assertEquals(0, "receiver".receiverLambda(1))
        assertEquals(0, replacesOwnDefault)
    }

    @Test
    fun classParameterWithoutADefaultKeepsItsDeclaredOne() {
        assertEquals(Sample(7), NoOpClassParameters.keepsOwnDefault)
    }

    @Test
    fun varargClassParameterIsLeftEmpty() {
        assertEquals(0, NoOpVarargClass.first)
        assertEquals(0, NoOpVarargClass.rest.size)
    }

    @Test
    fun originalSampleDeclarationsStillWork() {
        val classNoOp: SampleClass = NoOpSampleClass
        classNoOp.sampleMethod()
        assertEquals(false, classNoOp.sampleMethod1(Unit))

        val companionNoOp: SampleCompanionClass = SampleCompanionClass.NoOp
        assertEquals(false, companionNoOp.sampleMethod1(Unit))

        assertEquals(false, NoOpSampleInterface.sampleMethod1(Unit))
        assertEquals(false, SampleCompanionInterface.NoOp.sampleMethod1(Unit))
    }

    @Test
    fun directNoOpIsASingleInstance() {
        assertSame(NoOpScalars, NoOpScalars)
    }

    private fun <T> runSuspending(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
        return result!!.getOrThrow()
    }
}
