package dev.drewhamilton.poko.build

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig

@Suppress("unused") // Invoked reflectively by Gradle.
class PokoBuildPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.add(
            PokoBuildExtension::class.java,
            "pokoBuild",
            PokoBuildExtensionImpl(target)
        )

        commonKotlinConfiguration(target)
    }

    private fun commonKotlinConfiguration(project: Project) {
        project.tasks.withType(KotlinCompile::class.java).configureEach {
            kotlinOptions.freeCompilerArgs += "-progressive"
        }
    }

    private class PokoBuildExtensionImpl(private val project: Project) : PokoBuildExtension {
        override fun publishing() {
            // TODO Use version catalog references here.
            project.pluginManager.apply("com.vanniktech.maven.publish")
            project.pluginManager.apply("org.jetbrains.dokka")

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

        override fun generateArtifactInfo(basePackage: String) {
            val generateArtifactInfoProvider = project.tasks.register(
                "generateArtifactInfo",
                Copy::class.java,
                GenerateArtifactInfoAction(project, basePackage),
            )
            generateArtifactInfoProvider.configure {
                from(project.rootProject.layout.projectDirectory.dir("artifact-info-template"))
                into(project.layout.buildDirectory.dir("generated/source/artifact-info-template/main"))
            }
            project.extensions.configure<SourceSetContainer>("sourceSets") {
                getByName("main").java.srcDir(generateArtifactInfoProvider)
            }
        }

        class GenerateArtifactInfoAction(
            private val project: Project,
            private val basePackage: String,
        ) : Action<Copy> {
            override fun execute(t: Copy) {
                t.expand(
                    mapOf(
                        "basePackage" to basePackage,
                        "publishGroup" to "${project.group}",
                        "publishVersion" to "${project.version}",
                        "annotationsArtifact" to artifactIdForProject("poko-annotations"),
                        "compilerPluginArtifact" to artifactIdForProject("poko-compiler-plugin"),
                        "gradlePluginArtifact" to artifactIdForProject("poko-gradle-plugin"),
                    )
                )
                t.filteringCharset = "UTF-8"
            }

            private fun artifactIdForProject(projectName: String): Any {
                return project.rootProject.project(projectName).property("POM_ARTIFACT_ID")!!
            }
        }
    }
}
