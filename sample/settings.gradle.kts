rootProject.name = "sample"

include(":jvm")

// Compose requires Java 11; skip it on CI tests for lower JDKs
val ciJavaVersion = System.getenv()["ci_java_version"]
if (ciJavaVersion == null || Integer.valueOf(ciJavaVersion) >= 11) {
    include(":compose")
} else {
    logger.lifecycle("Testing on JDK $ciJavaVersion; skipping :compose module")
}

apply(from = "properties.gradle")
val isCi = System.getenv()["CI"] == "true"
if (!isCi) {
    // Use local Poko modules for non-CI builds:
    includeBuild("../.") {
        logger.lifecycle("Replacing Poko module dependencies with local projects")
        @Suppress("LocalVariableName") val publish_group: String by extra
        @Suppress("LocalVariableName") val publish_compiler_plugin_artifact: String by extra
        @Suppress("LocalVariableName") val publish_gradle_plugin_artifact: String by extra
        dependencySubstitution {
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
    @Suppress("UnstableApiUsage") // TODO: Remove in Gradle 8.0
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
