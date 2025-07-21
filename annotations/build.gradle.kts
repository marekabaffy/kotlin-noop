plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.publish)
}

group = "com.marekabaffy.kotlinnoop"
version = libs.versions.kotlinnoop

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()

    coordinates(group.toString(), "kotlinnoop-annotations", libs.versions.kotlinnoop.get())

    signAllPublications()

    pom {
        name.set("Kotlin NoOp Annotations")
        description.set("Annotations for generating no-op implementations")
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