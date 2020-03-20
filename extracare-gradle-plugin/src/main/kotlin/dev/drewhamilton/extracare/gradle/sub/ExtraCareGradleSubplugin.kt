package dev.drewhamilton.extracare.gradle.sub

import com.google.auto.service.AutoService
import dev.drewhamilton.extracare.gradle.ExtraCareGradlePlugin
import dev.drewhamilton.extracare.gradle.ExtraCarePluginExtension
import dev.drewhamilton.extracare.info.ArtifactInfo
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinGradleSubplugin::class)
class ExtraCareGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
        project.plugins.hasPlugin(ExtraCareGradlePlugin::class.java)

    override fun getCompilerPluginId(): String = ArtifactInfo.COMPILER_PLUGIN_ARTIFACT

    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = ArtifactInfo.GROUP,
        artifactId = getCompilerPluginId(),
        version = ArtifactInfo.VERSION
    )

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {
        val extension =
            project.extensions.findByType(ExtraCarePluginExtension::class.java) ?: ExtraCarePluginExtension()

        // Add annotations as a dependency
        project.dependencies.add(
            "implementation",
            "${ArtifactInfo.GROUP}:${ArtifactInfo.ANNOTATIONS_ARTIFACT}:${ArtifactInfo.VERSION}"
        )

        return listOf(
            SubpluginOption(key = "enabled", value = extension.enabled.toString())
        )
    }
}
