plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.publish)
}

group = "com.marekabaffy.kotlinnoop"
version = libs.versions.kotlinnoop.get()

kotlin {
    jvmToolchain(17)
    compilerOptions {
        // Compilers read metadata at most one version ahead, so this sets the oldest Kotlin that can use @NoOp.
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
    }
}

dependencies {
    // The annotation needs nothing from the standard library at runtime, so it must not raise the consumer's version.
    compileOnly(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        // Holds the jars that the integration-test build tests. Root `clean` removes it.
        maven {
            name = "integrationTest"
            url = uri(rootProject.layout.buildDirectory.dir("integration-test-repo"))
        }
    }
}

tasks.withType<Jar>().configureEach {
    metaInf {
        from(rootProject.file("LICENSE"))
    }
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