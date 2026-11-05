package com.marekabaffy.kotlinnoop.integrationtest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublishedArtifactsTest {
    @Test
    fun interfaceNoOpReturnsDefaults() {
        assertNull(NoOpRepository.name)
        assertTrue(NoOpRepository.list().isEmpty())
        assertTrue(NoOpRepository.emptyList().isEmpty())
    }

    @Test
    fun classNoOpHasDefaultCallbacks() {
        NoOpCallbacks.onClick()
        assertEquals(0, NoOpCallbacks.onLongClick("value"))
    }

    @Test
    fun companionNoOpReturnsDefaults() {
        assertEquals(0, WithCompanion.NoOp.value())
    }

    @Test
    fun nestedNoOpReturnsDefaults() {
        assertEquals(0, NoOpOuterInner.value())
    }
}
