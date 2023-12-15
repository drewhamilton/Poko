package dev.drewhamilton.poko.build

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig

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
        project.tasks.withType(KotlinCompile::class.java).configureEach {
            kotlinOptions.freeCompilerArgs += "-progressive"
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
                }
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

        override fun generateArtifactInfo(basePackage: String) {
            val generateArtifactInfoProvider = project.tasks.register(
                "generateArtifactInfo",
                Copy::class.java,
                GenerateArtifactInfoAction(basePackage, project.pokoGroupId, project.pokoVersion),
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
            private val basePackage: String,
            private val pokoGroupId: String,
            private val pokoVersion: String,
        ) : Action<Copy> {
            override fun execute(t: Copy) {
                t.expand(
                    mapOf(
                        "basePackage" to basePackage,
                        "publishGroup" to pokoGroupId,
                        "publishVersion" to pokoVersion,
                        "annotationsArtifact" to "poko-annotations",
                        "compilerPluginArtifact" to "poko-compiler-plugin",
                    )
                )
                t.filteringCharset = "UTF-8"
            }
        }
    }
}
