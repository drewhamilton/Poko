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
                    val publishGroup: String by extra
                    includeGroup(publishGroup)
                }
            }
        }
        mavenCentral()
        google()
    }

    resolutionStrategy {
        val publishVersion: String by extra
        eachPlugin {
            if (requested.id.id == "dev.drewhamilton.poko") {
                useVersion(publishVersion)
            }
        }
    }
}

rootProject.name = "PokoSample"

include(":jvm")

// Compose requires Java 11; skip it on CI tests for lower JDKs
private val ciJavaVersion = System.getenv()["ci_java_version"]
if (ciJavaVersion == null || Integer.valueOf(ciJavaVersion) >= 11) {
    include(":compose")
} else {
    logger.lifecycle("Testing on JDK $ciJavaVersion; skipping :compose module")
}

private val isCi = System.getenv()["CI"] == "true"
if (!isCi) {
    // Use local Poko modules for non-CI builds:
    includeBuild("../.") {
        logger.lifecycle("Replacing Poko module dependencies with local projects")
        val publishGroup: String by extra
        val publishAnnotationsArtifact: String by extra
        val publishCompilerPluginArtifact: String by extra
        val publishGradlePluginArtifact: String by extra
        dependencySubstitution {
            substitute(module("$publishGroup:$publishAnnotationsArtifact"))
                .using(project(":poko-annotations"))
                .because("Developers can see local changes reflected in the sample project")
            substitute(module("$publishGroup:$publishCompilerPluginArtifact"))
                .using(project(":poko-compiler-plugin"))
                .because("Developers can see local changes reflected in the sample project")
            substitute(module("$publishGroup:$publishGradlePluginArtifact"))
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
