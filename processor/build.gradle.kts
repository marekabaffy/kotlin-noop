plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvmToolchain(17)
    explicitApi()
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
