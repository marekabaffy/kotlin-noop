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
- Supports read-only and mutable collections, and `Sequence`
- Handles generic, nullable and function types, including `suspend` and receiver lambdas
- Handles generic functions, `vararg` parameters, extension members, nested declarations and inherited members

## 🔧 Setup

### 1. Add KSP Plugin

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.13"
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
    implementation("com.marekabaffy.kotlinnoop:kotlinnoop-annotations:0.0.1-SNAPSHOT")
    ksp("com.marekabaffy.kotlinnoop:kotlinnoop-ksp:0.0.1-SNAPSHOT")
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

The processor generates these defaults:

### Primitives & Basic Types

| Type                                 | Default Value |
|--------------------------------------|---------------|
| `String`                             | `""`          |
| `Int`, `Short`, `Byte`               | `0`           |
| `UInt`, `UShort`, `UByte`            | `0u`          |
| `Long`                               | `0L`          |
| `ULong`                              | `0uL`         |
| `Float`                              | `0f`          |
| `Double`                             | `0.0`         |
| `Boolean`                            | `false`       |
| `Char`                               | `'\u0000'`    |

### Arrays & Collections

| Type                                         | Default Value                         |
|----------------------------------------------|---------------------------------------|
| `ByteArray`, `IntArray`, etc.                | `byteArrayOf()`, `intArrayOf()`, etc. |
| `Array<T>`                                   | `emptyArray()`                        |
| `List`, `Collection`, `Iterable`             | `emptyList()`                         |
| `Set`                                        | `emptySet()`                          |
| `Map`                                        | `emptyMap()`                          |
| `Sequence`                                   | `emptySequence()`                     |
| `MutableList`, `MutableCollection`, `MutableIterable` | `mutableListOf()`            |
| `MutableSet`                                 | `mutableSetOf()`                      |
| `MutableMap`                                 | `mutableMapOf()`                      |

Star-projected collections and arrays use `Any?` as the element type, such as `mutableListOf<Any?>()` for
`MutableList<*>`.

### Special Types

| Type           | Default Value                              |
|----------------|--------------------------------------------|
| `(T) -> R`     | `{ _ -> ... }`                             |
| `T.() -> R`    | `{ ... }`                                  |
| Nullable types | `null`                                     |
| Other          | `throw UnsupportedOperationException(...)` |

Other types include your own classes, `Nothing`, unsigned arrays, a bare type parameter `T`, and an array of one
(`Array<T>`). Interface members with these types throw only when they are called.

Class constructor parameters are built as soon as the no-op is created, so a parameter with no known default keeps
its own declared default. If it has none, generation fails with an error naming the parameter. All other parameters
get the defaults above, replacing their declared defaults. A `vararg` constructor parameter is left empty.

## ⚠️ Limitations

`@NoOp` reports a compile error on:

- Generic interfaces and classes, because the no-op has no type arguments to use
- Private or protected declarations, including those nested in one
- Abstract, sealed and inner classes, and classes with a private or missing primary constructor
- Enums, objects and annotation classes

Functions and properties with context parameters (`context(logger: Logger) fun log()`) are not supported, because
KSP 2.3.13 does not report them. The generated override fails to compile with an "overrides nothing" error. A class
constructor parameter with a context function type (`context(C) (T) -> R`) fails to compile for the same reason.

Interface no-ops replace default method implementations too, so those methods also return the defaults above.

Nested declarations include the outer names: `Outer.Inner` generates `NoOpOuterInner`.
