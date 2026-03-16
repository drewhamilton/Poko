package dev.drewhamilton.poko.build

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.buildConfigField
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

private val Project.pokoGroupId get() = property("PUBLISH_GROUP") as String
private val Project.pokoVersion get() = property("PUBLISH_VERSION") as String

@Suppress("unused") // Invoked reflectively by Gradle.
class PokoBuildPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.group = target.pokoGroupId
        target.version = target.pokoVersion

        target.extensions.add(
            PokoBuildExtension::class.java,
            "pokoBuild",
            PokoBuildExtensionImpl(target)
        )

        commonKotlinConfiguration(target)
    }

    private fun commonKotlinConfiguration(project: Project) {
        project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
            compilerOptions.progressiveMode.convention(true)
        }
    }

    private class PokoBuildExtensionImpl(
        private val project: Project,
    ) : PokoBuildExtension {
        @OptIn(ExperimentalAbiValidation::class)
        override fun publishing(pomName: String) {
            project.pluginManager.apply("com.vanniktech.maven.publish")

            val publishing = project.extensions.getByName("publishing") as PublishingExtension
            publishing.repositories {
                maven {
                    name = "testing"
                    setUrl(project.rootProject.layout.buildDirectory.dir("localMaven"))
                }
            }

            val mavenPublishing = project.extensions.getByName("mavenPublishing") as MavenPublishBaseExtension
            mavenPublishing.apply {
                coordinates(project.pokoGroupId, project.name, project.pokoVersion)

                pom {
                    name.set(pomName)

                    description.set("A Kotlin compiler plugin for generating equals, hashCode, and toString for plain old Kotlin objects.")
                    url.set("https://github.com/drewhamilton/Poko")

                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }

                    scm {
                        url.set("https://github.com/drewhamilton/Poko/tree/main")
                        connection.set("scm:git:github.com/drewhamilton/Poko.git")
                        developerConnection.set("scm:git:ssh://github.com/drewhamilton/Poko.git")
                    }

                    developers {
                        developer {
                            id.set("drewhamilton")
                            name.set("Drew Hamilton")
                            email.set("software@drewhamilton.dev")
                        }
                    }
                }

                signAllPublications()
                publishToMavenCentral(
                    automaticRelease = true,
                )
            }

            project.pluginManager.apply("org.jetbrains.dokka")

            // Published modules should be explicit about their API visibility.
            val kotlinPluginHandler = Action<AppliedPlugin> {
                val kotlin = project.extensions.getByType<KotlinBaseExtension>()
                kotlin.explicitApi()
                val abiValidation = (kotlin as ExtensionAware).extensions.getByName("abiValidation")
                // KT-84630 KGP: AbiValidationMultiplatformExtension does not extend AbiValidationExtension
                if (abiValidation is AbiValidationMultiplatformExtension) {
                    abiValidation.enabled.set(true)
                } else {
                    abiValidation as AbiValidationExtension
                    abiValidation.enabled.set(true)
                }
                // KT-78525 KGP: abiValidation: check does not depend on checkLegacyAbi when enabled
                project.tasks.named("check") {
                    dependsOn(project.tasks.named("checkLegacyAbi"))
                }
            }
            project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm", kotlinPluginHandler)
            project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform", kotlinPluginHandler)
        }

        override fun generateBuildConfig(basePackage: String) {
            project.pluginManager.apply("com.github.gmazzo.buildconfig")

            val buildConfig = project.extensions.getByName("buildConfig") as BuildConfigExtension
            buildConfig.apply {
                packageName(basePackage)
                buildConfigField("GROUP", project.pokoGroupId)
                buildConfigField("VERSION", project.pokoVersion)
                buildConfigField("ANNOTATIONS_ARTIFACT", "poko-annotations")
                buildConfigField("COMPILER_PLUGIN_ARTIFACT", "poko-compiler-plugin")

                buildConfigField("COMPILER_PLUGIN_ID", "dev.drewhamilton.poko")

                buildConfigField("POKO_ENABLED_OPTION_NAME", "enabled")
                buildConfigField("DEFAULT_POKO_ENABLED", true)

                buildConfigField("POKO_ANNOTATION_OPTION_NAME", "pokoAnnotation")
                buildConfigField("DEFAULT_POKO_ANNOTATION", "dev/drewhamilton/poko/Poko")
            }
        }

        override fun enableBackwardsCompatibility(
            lowestSupportedKotlinVersion: KotlinVersion,
            lowestSupportedKotlinJvmVersion: KotlinVersion,
        ) {
            project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
                compilerOptions {
                    val actualKotlinVersion = if (this is KotlinJvmCompilerOptions) {
                        lowestSupportedKotlinJvmVersion
                    } else {
                        lowestSupportedKotlinVersion
                    }
                    apiVersion.set(actualKotlinVersion)
                    languageVersion.set(actualKotlinVersion)

                    if (actualKotlinVersion != KotlinVersion.DEFAULT) {
                        // This mode has no effect when targeting old api/language versions.
                        progressiveMode.set(false)
                    }
                }
            }
        }
    }
}
