plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvmToolchain(17)
    explicitApi()
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
        freeCompilerArgs.add("-Xjdk-release=8")
        // Compilers read metadata at most one version ahead, so this sets the oldest Kotlin that can use @NoOp.
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
    }
}

// Sets the Java version in the published Gradle metadata, which otherwise follows the JDK 17 toolchain.
tasks.compileJava {
    options.release = 8
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
