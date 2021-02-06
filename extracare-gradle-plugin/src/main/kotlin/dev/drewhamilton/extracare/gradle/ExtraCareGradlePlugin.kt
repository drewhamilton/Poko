package dev.drewhamilton.extracare.gradle

import dev.drewhamilton.extracare.info.ArtifactInfo
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class ExtraCareGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("extraCare", ExtraCarePluginExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        when (kotlinCompilation.platformType) {
            KotlinPlatformType.jvm, KotlinPlatformType.androidJvm -> true
            else -> false
        }

    fun temp(): Nothing = TODO("Remove this")

    override fun getCompilerPluginId(): String = ArtifactInfo.COMPILER_PLUGIN_ARTIFACT

    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = ArtifactInfo.GROUP,
        artifactId = getCompilerPluginId(),
        version = ArtifactInfo.VERSION
    )

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(ExtraCarePluginExtension::class.java)

        if (extension.dataApiAnnotation.get() == DEFAULT_DATA_API_ANNOTATION) {
            // Add default annotation as a dependency
            project.dependencies.add(
                "implementation",
                "${ArtifactInfo.GROUP}:${ArtifactInfo.ANNOTATIONS_ARTIFACT}:${ArtifactInfo.VERSION}"
            )
        }

        return project.provider {
            listOf(
                SubpluginOption(key = "enabled", value = extension.enabled.get().toString()),
                SubpluginOption(key = "dataApiAnnotation", value = extension.dataApiAnnotation.get().toString()),
            )
        }
    }

    internal companion object {
        internal const val DEFAULT_DATA_API_ANNOTATION = "dev.drewhamilton.extracare.DataApi"
    }
}
