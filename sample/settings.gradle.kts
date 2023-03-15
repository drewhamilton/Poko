pluginManagement {
    val isCi = System.getenv()["CI"] == "true"
    extra["isCi"] = isCi

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
                    @Suppress("LocalVariableName") val publish_group: String by extra
                    includeGroup(publish_group)
                }
            }
        }
        mavenCentral()
        google()
    }
}

rootProject.name = "sample"

include(":jvm")

// Compose requires Java 11; skip it on CI tests for lower JDKs
private val ciJavaVersion = System.getenv()["ci_java_version"]
extra["ciJavaVersion"] = ciJavaVersion
if (ciJavaVersion == null || Integer.valueOf(ciJavaVersion) >= 11) {
    include(":compose")
} else {
    logger.lifecycle("Testing on JDK $ciJavaVersion; skipping :compose module")
}

private val isCi: Boolean by extra
if (!isCi) {
    // Use local Poko modules for non-CI builds:
    includeBuild("../.") {
        logger.lifecycle("Replacing Poko module dependencies with local projects")
        @Suppress("LocalVariableName") val publish_group: String by extra
        @Suppress("LocalVariableName") val publish_annotations_artifact: String by extra
        @Suppress("LocalVariableName") val publish_compiler_plugin_artifact: String by extra
        @Suppress("LocalVariableName") val publish_gradle_plugin_artifact: String by extra
        dependencySubstitution {
            substitute(module("$publish_group:$publish_annotations_artifact"))
                .using(project(":poko-annotations"))
                .because("Developers can see local changes reflected in the sample project")
            substitute(module("$publish_group:$publish_compiler_plugin_artifact"))
                .using(project(":poko-compiler-plugin"))
                .because("Developers can see local changes reflected in the sample project")
            substitute(module("$publish_group:$publish_gradle_plugin_artifact"))
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
