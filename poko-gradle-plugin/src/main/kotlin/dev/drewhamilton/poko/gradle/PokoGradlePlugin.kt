package dev.drewhamilton.poko.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class PokoGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("poko", PokoPluginExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        when (kotlinCompilation.platformType) {
            KotlinPlatformType.jvm, KotlinPlatformType.androidJvm -> true
            else -> false
        }

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

        val annotationDependency = when (extension.pokoAnnotation.get()) {
            DEFAULT_POKO_ANNOTATION -> ArtifactInfo.annotationsDependency
            else -> null
        }
        if (annotationDependency != null) {
            project.dependencies.add(
                kotlinCompilation.compileOnlyConfigurationName,
                annotationDependency,
            )
        }

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
