plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
    explicitApi()
    compilerOptions {
        // Older compilers can warn about things newer ones accept, so only the current version is strict.
        allWarningsAsErrors = !providers.gradleProperty("kotlinVersion").isPresent
    }
}

dependencies {
    implementation("com.marekabaffy.kotlinnoop:kotlinnoop-annotations:${libs.versions.kotlinnoop.get()}")
    ksp("com.marekabaffy.kotlinnoop:kotlinnoop-ksp:${libs.versions.kotlinnoop.get()}")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
