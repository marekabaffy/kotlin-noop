# Changelog

## Unreleased

## 0.1.0

First release.

- `@NoOp` generates no-op implementations for interfaces, classes with a primary constructor, and their companion objects.
- Supports generic, nullable and function types, `vararg` parameters, generic functions, extension members, nested
  declarations and inherited members.
- Reports a compile error for declarations that cannot have a no-op.
- Requires JDK 17, Kotlin 2.2 and KSP 2.3.12 or newer.
