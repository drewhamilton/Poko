package dev.drewhamilton.extracare.gradle

import dev.drewhamilton.extracare.info.ArtifactInfo
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("unused") // Referenced in extracare-gradle-plugin/build.gradle
class ExtraCareGradlePlugin : KotlinCompilerPluginSupportPlugin {

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
        val extension = project.extensions.findByType(ExtraCarePluginExtension::class.java)
            ?: ExtraCarePluginExtension()

        // Add annotations as a dependency
        project.dependencies.add(
            "implementation",
            "${ArtifactInfo.GROUP}:${ArtifactInfo.ANNOTATIONS_ARTIFACT}:${ArtifactInfo.VERSION}"
        )

        return project.provider {
            listOf(
                SubpluginOption(key = "enabled", value = extension.enabled.toString())
            )
        }
    }
}
