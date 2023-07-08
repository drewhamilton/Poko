pluginManagement {
    includeBuild("build-support")

    repositories {
        mavenCentral()

        // KSP:
        google()
    }
}

plugins {
    id("dev.drewhamilton.poko.settings")
}

rootProject.name = "Poko"

include(
    ":poko-compiler-plugin",
    ":poko-annotations",
    ":poko-gradle-plugin",
)
