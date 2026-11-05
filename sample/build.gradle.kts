plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.annotations)

    ksp(projects.processor)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}