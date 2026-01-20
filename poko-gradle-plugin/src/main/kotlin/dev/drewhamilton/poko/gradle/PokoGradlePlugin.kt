package dev.drewhamilton.poko.gradle

import dev.drewhamilton.poko.gradle.BuildConfig.DEFAULT_POKO_ANNOTATION
import dev.drewhamilton.poko.gradle.BuildConfig.DEFAULT_POKO_ENABLED
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class PokoGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        val extension = target.extensions.create("poko", PokoPluginExtension::class.java)
        extension.enabled.convention(DEFAULT_POKO_ENABLED)
        extension.pokoAnnotation.convention(DEFAULT_POKO_ANNOTATION)

        val pokoAnnotationDependency = extension.pokoAnnotation.map {
            when (it) {
                DEFAULT_POKO_ANNOTATION -> target.dependencyFactory.create(BuildConfig.annotationsDependency)
                else -> null
            }
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlin = target.extensions.getByName("kotlin") as KotlinSourceSetContainer
            val commonMainSourceSet = kotlin.sourceSets.getByName(COMMON_MAIN_SOURCE_SET_NAME)

            target.configurations.named(commonMainSourceSet.implementationConfigurationName).configure {
                it.dependencies.addLater(pokoAnnotationDependency)
            }
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val sourceSets = target.extensions.getByName("sourceSets") as SourceSetContainer
            sourceSets.configureEach { sourceSet ->
                target.configurations.named(sourceSet.implementationConfigurationName).configure {
                    it.dependencies.addLater(pokoAnnotationDependency)
                }
            }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.COMPILER_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.GROUP,
        artifactId = BuildConfig.COMPILER_PLUGIN_ARTIFACT,
        version = BuildConfig.VERSION
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PokoPluginExtension::class.java)

        val optionsProvider = project.objects.listProperty(SubpluginOption::class.java)
        optionsProvider.add(
            extension.enabled.map {
                SubpluginOption(
                    key = BuildConfig.POKO_ENABLED_OPTION_NAME,
                    value = it.toString(),
                )
            }
        )
        optionsProvider.add(
            extension.pokoAnnotation.map {
                SubpluginOption(
                    key = BuildConfig.POKO_ANNOTATION_OPTION_NAME,
                    value = it,
                )
            }
        )

        return optionsProvider
    }

    private val BuildConfig.annotationsDependency: String
        get() = "$GROUP:$ANNOTATIONS_ARTIFACT:$VERSION"
}
