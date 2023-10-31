import dev.drewhamilton.poko.sample.build.jvmToolchainLanguageVersion
import dev.drewhamilton.poko.sample.build.kotlinJvmTarget
import dev.drewhamilton.poko.sample.build.resolvedJavaVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val ciJavaVersion = dev.drewhamilton.poko.sample.build.ciJavaVersion
    if (ciJavaVersion == null || ciJavaVersion >= 17) {
        alias(libs.plugins.android.library) apply false
    }
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    id("dev.drewhamilton.poko") apply false
}
apply(from = "properties.gradle")

logger.lifecycle("Targeting Java version $resolvedJavaVersion")

allprojects {
    repositories {
        if (System.getenv()["CI"] == "true") {
            logger.lifecycle("Resolving ${this@allprojects} Poko dependencies from MavenLocal")
            exclusiveContent {
                forRepository { mavenLocal() }
                filter {
                    includeGroup(property("GROUP") as String)
                }
            }
        }
        mavenCentral()

        if (rootProject.property("kotlin_dev_version_enabled") == "true") {
            logger.lifecycle("Adding Kotlin dev repository for ${this@allprojects}")
            maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
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
