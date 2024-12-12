package dev.drewhamilton.poko.build

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.kotlin.dsl.buildConfigField
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig
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
        override fun publishing(pomName: String) {
            project.pluginManager.apply("com.vanniktech.maven.publish")

            val mavenPublishing = project.extensions.getByName("mavenPublishing") as MavenPublishBaseExtension
            @Suppress("UnstableApiUsage")
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
                    host = SonatypeHost.DEFAULT,
                    automaticRelease = true,
                )
            }

            project.pluginManager.apply("org.jetbrains.dokka")
            project.pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")

            // Published modules should be explicit about their API visibility.
            val kotlinPluginHandler = Action<AppliedPlugin> {
                val kotlin = project.extensions.getByType(
                    KotlinTopLevelExtensionConfig::class.java
                )
                kotlin.explicitApi()
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
                buildConfigField("DEFAULT_POKO_ENABLED", true)
                buildConfigField("DEFAULT_POKO_ANNOTATION", "dev/drewhamilton/poko/Poko")
                buildConfigField("SKIP_ANNOTATION_SHORT_NAME", "Skip")
            }
        }
    }
}
