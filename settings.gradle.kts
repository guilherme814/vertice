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

rootProject.name = "NEXUS"

include(":app")
include(":core")
include(":core-ui")
include(":core-network")
include(":core-database")
include(":core-security")
include(":feature-auth")
include(":feature-chat")
include(":feature-calls")
include(":feature-screen-share")
include(":feature-communities")
include(":feature-profile")
include(":feature-moments")
include(":feature-settings")
include(":feature-media")
