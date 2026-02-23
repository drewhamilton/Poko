pluginManagement {
    val isCi = System.getenv()["CI"] == "true"

    apply(from = "properties.gradle")

    if (!isCi) {
        includeBuild("../.")
    }

    repositories {
        if (isCi) {
            logger.lifecycle("Resolving buildscript Poko dependencies from MavenLocal")
            exclusiveContent {
                forRepository { mavenLocal() }
                filter {
                    val publishGroup = extra["PUBLISH_GROUP"] as String
                    includeGroup(publishGroup)
                }
            }
        }
        mavenCentral()
        google()

        // foojay plugin:
        gradlePluginPortal()

        if (extra.has("kotlin_dev_repository")) {
            val kotlinDevRepository = extra["kotlin_dev_repository"]!!
            logger.lifecycle("Adding <$kotlinDevRepository> repository for plugins")
            maven { url = uri(kotlinDevRepository) }
        }
    }

    resolutionStrategy {
        val publishVersion = extra["PUBLISH_VERSION"] as String
        eachPlugin {
            if (requested.id.id == "dev.drewhamilton.poko") {
                useVersion(publishVersion)
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "PokoSample"

include(":jvm")

// Kotlin 2.3 does not support language target 1.9 for multiplatform:
private val ciKotlinLanguageVersion = System.getenv()["ORG_GRADLE_PROJECT_pokoSample_kotlinLanguageVersion"]
if (ciKotlinLanguageVersion?.startsWith("1") == true) {
    logger.lifecycle("Language version $ciKotlinLanguageVersion; skipping :mpp module")
} else {
    include(":mpp")
}

include(":compose")

private val isCi = System.getenv()["CI"] == "true"
if (!isCi) {
    // Use local Poko modules for non-CI builds:
    includeBuild("../.") {
        logger.lifecycle("Replacing Poko module dependencies with local projects")
        val publishGroup: String = extra["PUBLISH_GROUP"] as String
        dependencySubstitution {
            substitute(module("$publishGroup:${extra["poko-annotations.POM_ARTIFACT_ID"]}"))
                .using(project(":poko-annotations"))
                .because("Developers can see local changes reflected in the sample project")
            substitute(module("$publishGroup:${extra["poko-compiler-plugin.POM_ARTIFACT_ID"]}"))
                .using(project(":poko-compiler-plugin"))
                .because("Developers can see local changes reflected in the sample project")
            substitute(module("$publishGroup:${extra["poko-gradle-plugin.POM_ARTIFACT_ID"]}"))
                .using(project(":poko-gradle-plugin"))
                .because("Developers can see local changes reflected in the sample project")
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))

            fun String.nullIfBlank(): String? = if (isNullOrBlank()) null else this

            // Compile sample project with different Kotlin version than Poko, if provided:
            val localKotlinVersionOverride: String? = null
            val kotlinVersionOverride = localKotlinVersionOverride
                ?: System.getenv()["poko_sample_kotlin_version"]?.nullIfBlank()
            kotlinVersionOverride?.let { kotlinVersion ->
                version("kotlin", kotlinVersion)
            }
        }
    }
}

