pluginManagement {
    includeBuild("build-support")

    //region TODO: Move this to build-support
    val file: File = file("gradle.properties")
    val properties = java.util.Properties()
    file.inputStream().use {
        properties.load(it)
    }
    properties.forEach { (key, value) ->
        extra[key.toString()] = value
    }
    //endregion

    repositories {
        mavenCentral()

        // KSP:
        google()

        if (extra["kotlin_dev_version_enabled"] == "true") {
            logger.lifecycle("Adding Kotlin dev repository for plugins")
            maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
        }
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
    ":poko-tests",
    ":poko-tests:performance",
)
