import dev.drewhamilton.poko.build.setUpLocalSigning
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Note: kotlin-jvm and kotlin-multiplatform plugins are added implicitly via build-support
    alias(libs.plugins.ksp) apply false
}

allprojects {
    setUpLocalSigning()

    repositories {
        mavenCentral()

        // KSP:
        google()

        val snapshotsRepository = rootProject.findProperty("snapshots_repository")
        if (snapshotsRepository != null) {
            logger.lifecycle("Adding <$snapshotsRepository> repository for ${this@allprojects}")
            maven { url = uri(snapshotsRepository) }
        }
        val kotlinDevRepository = rootProject.findProperty("kotlin_dev_repository")
        if (kotlinDevRepository != null) {
            logger.lifecycle("Adding <$kotlinDevRepository> repository for ${this@allprojects}")
            maven { url = uri(kotlinDevRepository) }
        }
    }

    // The tests vary their own JVM targets among multiple targets. Do not overwrite them.
    if (path !in setOf(":poko-tests")) {
        val kotlinPluginHandler: AppliedPlugin.() -> Unit = {
            val javaVersion = JavaVersion.VERSION_1_8
            project.tasks.withType<JavaCompile>().configureEach {
                sourceCompatibility = javaVersion.toString()
                targetCompatibility = javaVersion.toString()
            }
            project.tasks.withType<KotlinCompile>().configureEach {
                compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
                    freeCompilerArgs.add("-Xjdk-release=$javaVersion")
                }
            }
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm", kotlinPluginHandler)
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform", kotlinPluginHandler)
    }
}
