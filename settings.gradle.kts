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

rootProject.name = "DocForge"

include(
    ":app",
    ":baselineprofile",
    ":core:ui",
    ":core:domain",
    ":core:storage",
    ":core:pdf",
    ":core:opencv",
    ":feature:converter",
    ":feature:history",
    ":feature:scanner",
    ":feature:pdf-tools"
)
