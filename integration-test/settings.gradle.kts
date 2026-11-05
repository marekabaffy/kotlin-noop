rootProject.name = "kotlin-noop-integration-test"

pluginManagement {
    repositories {
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri(file("../build/integration-test-repo"))
            content {
                includeGroup("com.marekabaffy.kotlinnoop")
            }
        }
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
            // -PkotlinVersion and -PkspVersion test other versions, such as the oldest ones the README promises.
            providers.gradleProperty("kotlinVersion").orNull?.let { version("kotlin", it) }
            providers.gradleProperty("kspVersion").orNull?.let { version("ksp", it) }
            providers.gradleProperty("releaseVersion").orNull?.let { version("kotlinnoop", it) }
        }
    }
}
