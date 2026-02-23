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

        // buildconfig and foojay plugins:
        gradlePluginPortal()

        if (extra.has("kotlin_dev_repository")) {
            val kotlinDevRepository = extra["kotlin_dev_repository"]!!
            logger.lifecycle("Adding <$kotlinDevRepository> repository for plugins")
            maven { url = uri(kotlinDevRepository) }
        }
    }
}

plugins {
    id("dev.drewhamilton.poko.settings")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Poko"

include(
    ":poko-compiler-plugin",
    ":poko-annotations",
    ":poko-gradle-plugin",
    ":poko-tests",
    ":poko-tests:performance",
)
