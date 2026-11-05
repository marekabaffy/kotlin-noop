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
    versionCatalogs {
        create("libs") {
            // The release workflow passes the tag, so the catalog can keep its snapshot version between releases.
            providers.gradleProperty("releaseVersion").orNull?.let { version("kotlinnoop", it) }
        }
    }
}

include(
    "annotations",
    "processor",
    "sample"
)


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")