import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("dev.drewhamilton.poko") apply false
}

private val resolvedJavaVersion = when (val ciJavaVersion = System.getenv()["ci_java_version"]) {
    null -> JavaVersion.VERSION_11
    "8", "9", "10" -> JavaVersion.valueOf("VERSION_1_$ciJavaVersion")
    else -> JavaVersion.valueOf("VERSION_$ciJavaVersion")
}
extra["resolvedJavaVersion"] = resolvedJavaVersion
logger.lifecycle("Targeting Java version $resolvedJavaVersion")

extra["kotlinJvmTarget"] = when (resolvedJavaVersion) {
    JavaVersion.VERSION_1_8 -> JvmTarget.JVM_1_8
    else -> JvmTarget.valueOf("JVM_${resolvedJavaVersion.majorVersion}")
}

allprojects {
    repositories {
        if (System.getenv()["CI"] == "true") {
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
