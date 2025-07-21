plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    alias(libs.plugins.maven.publish)
}

group = "com.marekabaffy.kotlinnoop"
version = libs.versions.kotlinnoop

dependencies {
    implementation(libs.kotlin.ksp.api)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
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
