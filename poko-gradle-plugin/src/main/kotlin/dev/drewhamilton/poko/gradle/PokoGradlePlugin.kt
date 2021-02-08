package dev.drewhamilton.poko.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class PokoGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("poko", PokoPluginExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        when (kotlinCompilation.platformType) {
            KotlinPlatformType.jvm, KotlinPlatformType.androidJvm -> true
            else -> false
        }

    override fun getCompilerPluginId(): String = ArtifactInfo.COMPILER_PLUGIN_ARTIFACT

    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = ArtifactInfo.GROUP,
        artifactId = getCompilerPluginId(),
        version = ArtifactInfo.VERSION
    )

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PokoPluginExtension::class.java)

        val pokoAnnotationName = extension.pokoAnnotation.get()
        if (pokoAnnotationName == DEFAULT_POKO_ANNOTATION) {
            // Add default annotation as a dependency
            project.dependencies.add(
                "compileOnly",
                "${ArtifactInfo.GROUP}:${ArtifactInfo.ANNOTATIONS_ARTIFACT}:${ArtifactInfo.VERSION}"
            )
        } else if (pokoAnnotationName == LEGACY_DATA_API_ANNOTATION) {
            // Add legacy default annotation as a dependency
            project.dependencies.add("implementation", "dev.drewhamilton.extracare:data-api-annotations:0.6.0")
        }

        return project.provider {
            listOf(
                SubpluginOption(key = "enabled", value = extension.enabled.get().toString()),
                SubpluginOption(key = "pokoAnnotation", value = extension.pokoAnnotation.get().toString()),
            )
        }
    }
}
