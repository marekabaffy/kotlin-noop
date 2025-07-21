package com.marekabaffy.kotlinnoop.annotations

/**
 * Generates no-operation implementations for interfaces and classes.
 * 
 * - Interfaces → `object NoOp{Name} : {Name}`
 * - Classes → `val NoOp{Name} = {Name}(defaultValues)`
 * - Companion objects → `val {Name}.Companion.NoOp: {Name}`
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NoOp