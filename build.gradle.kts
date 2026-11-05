plugins {
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

group = "com.marekabaffy.kotlinnoop"
version = libs.versions.kotlinnoop.get()

