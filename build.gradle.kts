plugins {
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.jvm)
}

group = "com.marekabaffy.kotlinnoop"
version = libs.versions.kotlinnoop.get()

kotlin {
    jvmToolchain(21)
}

