# Kotlin NoOp Generator

A lightweight KSP processor that automatically generates no-operation (stub) implementations for your Kotlin interfaces
and classes.

## ✨ Features

**Code Generation:**

- Generates no-op object implementations for interfaces
- Generates no-op `val` instances for classes (with primary constructors)
- Supports clean `Companion.NoOp` extension pattern

**Type Support:**

- Handles all primitive types and their arrays
- Supports collections (`List`, `Set`, `Map`, `Sequence`)
- Handles function types and lambdas with proper signatures

## 🔧 Setup

### 1. Add KSP Plugin

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
}
```

### 2. Add Snapshot Repository

Since this library is currently available as a snapshot version, add the Maven Central snapshots repository:

```kotlin
repositories {
    maven {
        name = "Central Portal Snapshots"
        url = URI("https://central.sonatype.com/repository/maven-snapshots/")

        content {
            includeModule("com.marekabaffy.kotlinnoop", "kotlinnoop-annotations")
            includeModule("com.marekabaffy.kotlinnoop", "kotlinnoop-ksp")
        }
    }
}
```

### 3. Add Dependencies

```kotlin
dependencies {
    implementation("com.marekabaffy.kotlinnoop:annotations:0.0.1-SNAPSHOT")
    ksp("com.marekabaffy.kotlinnoop:processor:0.0.1-SNAPSHOT")
}
```

## 🚀 Usage

Add the `@NoOp` annotation to interfaces, classes, or companion objects to generate no-op implementations.

### Interfaces

#### Direct Annotation

```kotlin
@NoOp
interface ViewCallbacks {
    fun onClick()
    fun onLongClick(item: String)
}

// Generates: object NoOpViewCallbacks : ViewCallbacks { ... }
```

#### Companion Object Pattern

```kotlin
interface ViewCallbacks {
    fun onClick()
    fun onLongClick(item: String)

    @NoOp
    companion object
}

// Generates: val ViewCallbacks.Companion.NoOp: ViewCallbacks
```

### Classes

#### Direct Annotation

Generates a `val` instance using default values for primary constructor parameters.

```kotlin
@NoOp
data class ViewCallbacks(
    val onClick: () -> Unit,
    val onLongClick: (String) -> Unit
)

// Generates: val NoOpViewCallbacks = ViewCallbacks(onClick = {}, ...)
```

#### Companion Object Pattern

```kotlin
data class ViewCallbacks(
    val onClick: () -> Unit,
    val onLongClick: (String) -> Unit
) {
    @NoOp
    companion object
}

// Generates: val ViewCallbacks.Companion.NoOp: ViewCallbacks
```

## 🧩 Default Values

The processor generates sensible defaults for all supported types:

### Primitives & Basic Types

| Type                   | Default Value |
|------------------------|---------------|
| `String`               | `""`          |
| `Int`, `Short`, `Byte` | `0`           |
| `Long`                 | `0L`          |
| `Float`                | `0.0f`        |
| `Double`               | `0.0`         |
| `Boolean`              | `false`       |
| `Char`                 | `'\u0000'`    |

### Arrays & Collections

| Type                          | Default Value                         |
|-------------------------------|---------------------------------------|
| `ByteArray`, `IntArray`, etc. | `byteArrayOf()`, `intArrayOf()`, etc. |
| `List`, `MutableList`         | `emptyList()`                         |
| `Set`, `MutableSet`           | `emptySet()`                          |
| `Map`, `MutableMap`           | `emptyMap()`                          |
| `Sequence`                    | `emptySequence()`                     |

### Special Types

| Type           | Default Value                              |
|----------------|--------------------------------------------|
| `(T) -> R`     | `{ _ -> ... }`                             |
| Nullable types | `null`                                     |
| Other          | `throw UnsupportedOperationException(...)` |
