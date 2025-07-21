plugins {
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
    kotlin("jvm") version "2.2.0"
}

group = "com.marekabaffy.kotlinnoop"
version = libs.versions.kotlinnoop

kotlin {
    jvmToolchain(21)
}

