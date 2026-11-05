plugins {
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.jvm)
}

group = "com.marekabaffy.kotlinnoop"
version = libs.versions.kotlinnoop

kotlin {
    jvmToolchain(21)
}

