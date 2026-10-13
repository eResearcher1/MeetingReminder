pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()



    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // This line is important for resolving Gradle plugins



    }
}

rootProject.name = "MeetMinder"
include(":app")
 