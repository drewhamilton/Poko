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
                    val publishGroup = extra["GROUP"] as String
                    includeGroup(publishGroup)
                }
            }
        }
        mavenCentral()
        google()
    }

    resolutionStrategy {
        val publishVersion = extra["VERSION_NAME"] as String
        eachPlugin {
            if (requested.id.id == "dev.drewhamilton.poko") {
                useVersion(publishVersion)
            }
        }
    }
}

rootProject.name = "PokoSample"

include(":jvm")
include(":mpp")

// Android requires JDK 17; skip it on CI tests for lower JDKs
private val ciJavaVersion = System.getenv()["ci_java_version"]?.toInt()
if (ciJavaVersion == null || ciJavaVersion >= 17) {
    include(":compose")
} else {
    logger.lifecycle("Testing on JDK $ciJavaVersion; skipping :compose module")
}

private val isCi = System.getenv()["CI"] == "true"
if (!isCi) {
    // Use local Poko modules for non-CI builds:
    includeBuild("../.") {
        logger.lifecycle("Replacing Poko module dependencies with local projects")
        val publishGroup: String = extra["GROUP"] as String
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
        }
    }
}
