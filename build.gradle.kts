import dev.drewhamilton.poko.build.setUpLocalSigning
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
    alias(libs.plugins.ksp) apply false
}

allprojects {
    group = rootProject.property("GROUP")!!
    version = rootProject.property("VERSION_NAME")!!
    setUpLocalSigning()

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

    val kotlinPluginHandler: AppliedPlugin.() -> Unit = {
        val javaVersion = JavaVersion.VERSION_1_8
        project.tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = javaVersion.toString()
            targetCompatibility = javaVersion.toString()
        }
        project.tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
            }
        }
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm", kotlinPluginHandler)
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform", kotlinPluginHandler)
}
