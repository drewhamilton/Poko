import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    id("dev.drewhamilton.poko") apply false
}
apply(from = "properties.gradle")

val ciJavaVersion = "10"//System.getenv()["ci_java_version"]
val resolvedJavaVersion = when (ciJavaVersion) {
    null -> JavaVersion.VERSION_11
    "8", "9", "10" -> JavaVersion.valueOf("VERSION_1_$ciJavaVersion")
    else -> JavaVersion.valueOf("VERSION_$ciJavaVersion")
}
logger.lifecycle("Targeting Java version $resolvedJavaVersion")

val kotlinJvmTarget = when (resolvedJavaVersion) {
    JavaVersion.VERSION_1_8 -> JvmTarget.JVM_1_8
    else -> JvmTarget.valueOf("JVM_${resolvedJavaVersion.majorVersion}")
}

val jvmToolchainLanguageVersion = ciJavaVersion?.let { JavaLanguageVersion.of(ciJavaVersion.toInt()) }
allprojects {
    repositories {
        if (System.getenv()["CI"] == "true") {
            logger.lifecycle("Resolving ${this@allprojects} Poko dependencies from MavenLocal")
            exclusiveContent {
                forRepository { mavenLocal() }
                filter {
                    includeGroup(property("publishGroup") as String)
                }
            }
        }
        mavenCentral()
    }

    plugins.withId("org.jetbrains.kotlin.android") {
        if (jvmToolchainLanguageVersion == null) {
            with(extensions.getByType<LibraryExtension>()) {
                compileOptions {
                    sourceCompatibility(resolvedJavaVersion)
                    targetCompatibility(resolvedJavaVersion)
                }
            }
        } else {
            extensions.getByType<KotlinTopLevelExtension>().jvmToolchain {
                languageVersion.set(jvmToolchainLanguageVersion)
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        if (jvmToolchainLanguageVersion == null) {
            with(extensions.getByType<JavaPluginExtension>()) {
                sourceCompatibility = resolvedJavaVersion
                targetCompatibility = resolvedJavaVersion
            }
        } else {
            extensions.getByType<KotlinJvmProjectExtension>().jvmToolchain {
                languageVersion.set(jvmToolchainLanguageVersion)
            }
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(kotlinJvmTarget)
            freeCompilerArgs.add("-progressive")
        }
    }
}
