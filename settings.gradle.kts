rootProject.name = "kotlin-noop"

pluginManagement {
    repositories {
        mavenCentral()
    }
}

// https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:centralized-repository-declaration
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

include(
    "annotations",
    "processor",
    "sample"
)


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")