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

// -PVERSION_NAME tests a specific version, as the release workflow does. The default is the main build's version.
val noOpVersion = providers.gradleProperty("VERSION_NAME").orElse(
    providers.fileContents(layout.projectDirectory.file("../gradle.properties")).asText.map {
        Regex("^VERSION_NAME=(.+)$", RegexOption.MULTILINE).find(it)!!.groupValues[1]
    }
).get()

dependencies {
    implementation("com.marekabaffy.kotlinnoop:kotlinnoop-annotations:$noOpVersion")
    ksp("com.marekabaffy.kotlinnoop:kotlinnoop-ksp:$noOpVersion")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
