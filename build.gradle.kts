import dev.drewhamilton.poko.build.setUpLocalSigning
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator) apply false
    alias(libs.plugins.ksp) apply false
}

plugins.withType<NodeJsRootPlugin> {
    extensions.getByType<NodeJsRootExtension>().apply {
        // WASM requires a canary Node.js version. This is the last v22 nightly that supports
        // darwin-arm64, darwin-x64 and win-x64 artifacts:
        version = "22.0.0-nightly20240410c82f3c9e80"
        downloadBaseUrl = "https://nodejs.org/download/nightly"
    }
}

tasks.withType<KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
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
    if (path !in setOf(":poko-tests", ":poko-tests-without-k2")) {
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
