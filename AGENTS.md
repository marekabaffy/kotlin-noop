# kotlin-noop

A KSP processor that generates no-op implementations for types annotated with `@NoOp`.
[README.md](README.md) owns what it does and what it supports.

## Layout

- `annotations/`: the published `@NoOp` annotation. It targets Java 8 and Kotlin 2.2 and has no dependencies, so it
  doesn't restrict consumers.
- `processor/`: the published processor, with compile-level tests using kctfork.
- `sample/`: declarations the processor generates from, with runtime tests of the generated code.
- `integration-test/`: a separate build that tests the published jars the way a consumer uses them.

## Commands

Building needs JDK 17.

```shell
./gradlew build
./gradlew publishAllPublicationsToIntegrationTestRepository
./gradlew -p integration-test build
./gradlew -p integration-test build -PkotlinVersion=2.2.21 -PkspVersion=2.3.12
```

## Rules

- Generated code must compile for anything the processor accepts. Report a `@NoOp` error for what it can't support.
- When a default value or limitation changes, update the README's tables and add a test.
- The oldest supported versions appear in the README, `.github/workflows` and the error in
  `NoOpAnnotationProcessorProvider`. Change them together.
- Add user-visible changes to the `Unreleased` section of `CHANGELOG.md`.
- Never publish or push a tag without being asked. A Maven Central release can't be changed or deleted.
  [RELEASING.md](RELEASING.md) owns the process.
