plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

dependencies {
    implementation(projects.annotations)

    ksp(projects.processor)
}

tasks.test {
    useJUnitPlatform()
}