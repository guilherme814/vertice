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
    }
}

rootProject.name = "VerticeLauncher"

include(":app")
include(":domain")
include(":data")
include(":ui-core")
include(":feature-home")
include(":feature-drawer")
include(":feature-settings")
include(":feature-island")
