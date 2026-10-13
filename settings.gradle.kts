pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()



    }
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
 