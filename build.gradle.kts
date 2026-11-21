import dev.drewhamilton.poko.build.setUpLocalSigning
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Note: kotlin-jvm and kotlin-multiplatform plugins are added implicitly via build-support
    alias(libs.plugins.ksp) apply false
}

allprojects {
    setUpLocalSigning()

    // The tests vary their own JVM targets among multiple targets. Do not overwrite them.
    if (path !in setOf(":poko-tests", ":poko-gradle-plugin")) {
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
