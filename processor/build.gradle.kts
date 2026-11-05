plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    alias(libs.plugins.maven.publish)
}

group = "com.marekabaffy.kotlinnoop"
version = libs.versions.kotlinnoop.get()

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlin.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(kotlin("test"))
    testImplementation(projects.annotations)
    testImplementation(libs.kctfork.ksp)
}

tasks.test {
    useJUnitPlatform()
}

// kctfork brings its own KSP version; tests must run the same KSP as the build.
configurations.testRuntimeClasspath {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.devtools.ksp") useVersion(libs.versions.ksp.get())
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "kotlinnoop-ksp", libs.versions.kotlinnoop.get())

    pom {
        name.set("Kotlin NoOp KSP Processor")
        description.set("Kotlin Symbol Processing (KSP) processor for generating no-op implementations")
        url.set("https://github.com/marekabaffy/kotlin-noop")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("marekabaffy")
                name.set("Marek Abaffy")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/marekabaffy/kotlin-noop.git")
            developerConnection.set("scm:git:ssh://github.com/marekabaffy/kotlin-noop.git")
            url.set("https://github.com/marekabaffy/kotlin-noop")
        }
    }
}
