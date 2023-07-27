package dev.drewhamilton.poko.gradle

import dev.drewhamilton.poko.gradle.ArtifactInfo.DEFAULT_POKO_ANNOTATION
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class PokoGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        val extension = target.extensions.create("poko", PokoPluginExtension::class.java)

        target.afterEvaluate {
            val annotationDependency = when (extension.pokoAnnotation.get()) {
                DEFAULT_POKO_ANNOTATION -> ArtifactInfo.annotationsDependency
                else -> null
            }
            if (annotationDependency != null) {
                if (target.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                    val kotlin = target.extensions.getByName("kotlin") as KotlinSourceSetContainer
                    kotlin.sourceSets.getByName(COMMON_MAIN_SOURCE_SET_NAME) { sourceSet ->
                        sourceSet.dependencies {
                            implementation(annotationDependency)
                        }
                    }
                } else {
                    target.dependencies.add(IMPLEMENTATION_CONFIGURATION_NAME, annotationDependency)
                }
            }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = ArtifactInfo.COMPILER_PLUGIN_ARTIFACT

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = ArtifactInfo.GROUP,
        artifactId = getCompilerPluginId(),
        version = ArtifactInfo.VERSION
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PokoPluginExtension::class.java)

        return project.provider {
            listOf(
                SubpluginOption(key = "enabled", value = extension.enabled.get().toString()),
                SubpluginOption(key = "pokoAnnotation", value = extension.pokoAnnotation.get()),
            )
        }
    }

    private val ArtifactInfo.annotationsDependency: String
        get() = "$GROUP:$ANNOTATIONS_ARTIFACT:$VERSION"
}
