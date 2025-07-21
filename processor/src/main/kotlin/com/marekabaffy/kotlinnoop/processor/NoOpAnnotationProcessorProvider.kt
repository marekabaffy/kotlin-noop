package com.marekabaffy.kotlinnoop.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP provider that creates NoOpAnnotationProcessor instances.
 */
class NoOpAnnotationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return NoOpAnnotationProcessor(environment)
    }
}