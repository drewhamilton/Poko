package dev.drewhamilton.careful.gradle.sub

import com.google.auto.service.AutoService
import dev.drewhamilton.careful.gradle.ANNOTATIONS_ARTIFACT
import dev.drewhamilton.careful.gradle.CarefulGradlePlugin
import dev.drewhamilton.careful.gradle.CarefulPluginExtension
import dev.drewhamilton.careful.gradle.GROUP
import dev.drewhamilton.careful.gradle.VERSION
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinGradleSubplugin::class)
class CarefulGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
        project.plugins.hasPlugin(CarefulGradlePlugin::class.java)

    override fun getCompilerPluginId(): String = "careful-compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = GROUP,
            artifactId = getCompilerPluginId(),
            version = VERSION
        )

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {
        val extension = project.extensions.findByType(CarefulPluginExtension::class.java) ?: CarefulPluginExtension()

        // Add annotation as a dependency
        project.dependencies.add("implementation", "$GROUP:$ANNOTATIONS_ARTIFACT:$VERSION")

        return listOf(
            SubpluginOption(key = "enabled", value = extension.enabled.toString())
        )
    }
}
