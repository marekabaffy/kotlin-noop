plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

dependencies {
    implementation(projects.annotations)

    ksp(projects.processor)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}