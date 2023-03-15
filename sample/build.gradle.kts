buildscript {
    apply(from = "properties.gradle")

    extra["ciJavaVersion"] = System.getenv()["ci_java_version"]
    val ciJavaVersion: String? by extra

    extra["resolvedJavaVersion"] = ciJavaVersion ?: JavaVersion.VERSION_11.toString()
    val resolvedJavaVersion: String by extra
    logger.lifecycle("Targeting Java version $resolvedJavaVersion")

    extra["kotlinJvmTarget"] = if (resolvedJavaVersion == "8") "1.8" else resolvedJavaVersion

    extra["isCi"] = System.getenv()["CI"] == "true"
    val isCi: Boolean by extra
    @Suppress("LocalVariableName") val publish_group: String by extra
    @Suppress("LocalVariableName") val publish_gradle_plugin_artifact: String by extra
    @Suppress("LocalVariableName") val publish_version: String by extra
    repositories {
        if (isCi) {
            logger.lifecycle("Resolving buildscript Poko dependencies from MavenLocal")
            exclusiveContent {
                forRepository { mavenLocal() }
                filter { includeGroup(publish_group) }
            }
        }
        mavenCentral()
    }

    // FIXME: Apply in plugins block
    dependencies {
        classpath("$publish_group:$publish_gradle_plugin_artifact:$publish_version")
    }
}

val isCi: Boolean by extra
allprojects {
    repositories {
        if (isCi) {
            logger.lifecycle("Resolving ${this@allprojects} Poko dependencies from MavenLocal")
            exclusiveContent {
                forRepository { mavenLocal() }
                filter {
                    @Suppress("LocalVariableName") val publish_group: String by extra
                    includeGroup(publish_group)
                }
            }
        }
        mavenCentral()
    }
}
