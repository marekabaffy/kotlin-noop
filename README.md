# Kotlin NoOp

[![Maven Central](https://img.shields.io/maven-central/v/com.marekabaffy.kotlinnoop/kotlinnoop-annotations)](https://central.sonatype.com/artifact/com.marekabaffy.kotlinnoop/kotlinnoop-annotations)
[![CI](https://github.com/marekabaffy/kotlin-noop/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/marekabaffy/kotlin-noop/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A [KSP](https://github.com/google/ksp) processor that generates no-op implementations of Kotlin interfaces and classes.

Screen callbacks, previews, tests and optional dependencies often need an implementation that does nothing. Writing
one by hand means updating it whenever the type changes. Annotate the type with `@NoOp` and it stays in sync:

```kotlin
data class LoginCallbacks(
    val onSubmit: (email: String, password: String) -> Unit,
    val onForgotPassword: () -> Unit,
    val isValidEmail: (String) -> Boolean,
) {
    @NoOp
    companion object
}

// In a preview or a test:
LoginScreen(callbacks = LoginCallbacks.NoOp)
```

The processor generates this at build time:

```kotlin
val LoginCallbacks.Companion.NoOp: LoginCallbacks
    get() = LoginCallbacks(onSubmit = { _, _ -> }, onForgotPassword = { }, isValidEmail = { _ -> false })
```

## Download

The library is on Maven Central. Add the KSP plugin and both dependencies:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.13"
}

dependencies {
    implementation("com.marekabaffy.kotlinnoop:kotlinnoop-annotations:0.1.0")
    ksp("com.marekabaffy.kotlinnoop:kotlinnoop-ksp:0.1.0")
}
```

### Requirements

| Tool   | Minimum |
|--------|---------|
| JDK    | 17      |
| Kotlin | 2.2     |
| KSP    | 2.3.12  |

The annotations are JVM-only, so `@NoOp` works in Kotlin/JVM and Android code but not in Kotlin Multiplatform common
code.

## Usage

Put `@NoOp` on an interface, a class, or either one's companion object.

| Annotate              | You get                |
|-----------------------|------------------------|
| An interface or class | A top-level `NoOpName` |
| Its companion object  | `Name.NoOp`            |

`NoOpName` keeps one instance. `Name.NoOp` creates a new one each time it is read. The generated code has the same
visibility as your type, or `internal` if anything enclosing it is `internal`.

### Interfaces

Every function and property is overridden, including inherited ones and those with default implementations:

```kotlin
@NoOp
interface Analytics {
    val sessionId: String?
    fun track(event: String, properties: Map<String, String>)
    fun enabledFeatures(): List<String>
    suspend fun flush(): Boolean
    fun currentUser(): User
}
```

Generates:

```kotlin
object NoOpAnalytics : Analytics {
    override val sessionId: String?
        get() = null

    override fun track(event: String, properties: Map<String, String>) {
    }

    override fun enabledFeatures(): List<String> = emptyList()

    override suspend fun flush(): Boolean = false

    override fun currentUser(): User {
        throw UnsupportedOperationException("@NoOp has no default for User")
    }
}
```

`currentUser()` throws because there is no general way to create a `User`, so it only fails if it is called. Setting a
`var` property does nothing, and reading it still returns the default.

### Classes

The primary constructor is called with the defaults below:

```kotlin
@NoOp
class Settings(
    val retries: Int,
    val tags: Set<String>,
    val user: User = User(),
)

// Generates:
val NoOpSettings: Settings = Settings(retries = 0, tags = emptySet())
```

The instance is created when it is first used, so a parameter with no known default keeps its own declared default,
as `user` does here. If it has none, the build fails with an error naming the parameter. Known defaults replace
declared ones, and a `vararg` parameter is left empty.

### Nested types

Generated names include the outer names, so `Screen.Listener` generates `NoOpScreenListener`.

## Default values

| Type                                                  | Default                                |
|-------------------------------------------------------|----------------------------------------|
| Nullable types                                        | `null`                                 |
| `Boolean`                                             | `false`                                |
| `Byte`, `Short`, `Int`                                | `0`                                    |
| `UByte`, `UShort`, `UInt`                             | `0u`                                   |
| `Long`                                                | `0L`                                   |
| `ULong`                                               | `0uL`                                  |
| `Float`                                               | `0f`                                   |
| `Double`                                              | `0.0`                                  |
| `Char`                                                | `'\u0000'`                             |
| `String`                                              | `""`                                   |
| `ByteArray`, `IntArray` and other primitive arrays    | `byteArrayOf()`, `intArrayOf()` etc.   |
| `Array<T>`                                            | `emptyArray()`                         |
| `List`, `Collection`, `Iterable`                      | `emptyList()`                          |
| `Set`                                                 | `emptySet()`                           |
| `Map`                                                 | `emptyMap()`                           |
| `Sequence`                                            | `emptySequence()`                      |
| `MutableList`, `MutableCollection`, `MutableIterable` | `mutableListOf()`                      |
| `MutableSet`                                          | `mutableSetOf()`                       |
| `MutableMap`                                          | `mutableMapOf()`                       |
| Function types, including `suspend`                   | A lambda returning its return type's default |

Anything else has no default, including your own classes, `Nothing`, unsigned arrays, a bare type parameter `T` and
`Array<T>` of one. Interface members with these types throw `UnsupportedOperationException` when called.

Star-projected collections and arrays use `Any?`, such as `mutableListOf<Any?>()` for `MutableList<*>`.

## Limitations

The build fails with a `@NoOp` error for:

- Generic interfaces and classes, because a no-op has no type arguments to use.
- Private or protected declarations, including those nested in one.
- Abstract, sealed and inner classes, and classes with a private, protected or missing primary constructor.
- Companion objects of sealed interfaces. Annotate the sealed interface itself instead.
- Enums, objects and annotation classes.
- Two declarations in a package that would generate the same file name, ignoring case, such as `Outer.Inner` and
  `OuterInner`.

The build fails with a compiler error for:

- Functions and properties with context parameters, because KSP does not report them.
- A class constructor parameter with a context function type, because KSP does not mark those types.
- An interface with a member or parameter named `kotlin`, which hides the full names generated code uses.

## Development

Building needs JDK 17. `integration-test/` is a separate build that tests the published jars the way a consumer uses
them, so publish them first:

```shell
./gradlew build
./gradlew publishAllPublicationsToIntegrationTestRepository
./gradlew -p integration-test build
```

[RELEASING.md](RELEASING.md) covers releases, and [CHANGELOG.md](CHANGELOG.md) lists what changed in each one.

## License

[MIT](LICENSE)
