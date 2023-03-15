import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = rootProject.property("publish_group")!!
version = rootProject.property("publish_version")!!

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator) apply false
    alias(libs.plugins.ksp) apply false
}

apply(plugin = "binary-compatibility-validator")

allprojects {
    repositories {
        mavenCentral()

        // KSP:
        google()

        if (rootProject.property("snapshot_dependencies_enabled") == "true") {
            logger.lifecycle("Adding snapshots repository for ${this@allprojects}")
            maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        }
        if (rootProject.property("kotlin_dev_version_enabled") == "true") {
            logger.lifecycle("Adding Kotlin dev repository for ${this@allprojects}")
            maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        val javaVersion = JavaVersion.VERSION_1_8
        project.tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = javaVersion.toString()
            targetCompatibility = javaVersion.toString()
        }
        project.tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
                freeCompilerArgs.add("-progressive")
            }
        }
    }
}
