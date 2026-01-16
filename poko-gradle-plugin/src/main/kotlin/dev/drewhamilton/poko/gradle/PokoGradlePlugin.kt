package dev.drewhamilton.poko.gradle

import dev.drewhamilton.poko.gradle.BuildConfig.DEFAULT_POKO_ANNOTATION
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
                DEFAULT_POKO_ANNOTATION -> BuildConfig.annotationsDependency
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
                    if (target.plugins.hasPlugin("org.gradle.java-test-fixtures")) {
                        target.dependencies.add("testFixturesImplementation", annotationDependency)
                    }
                    target.dependencies.add(IMPLEMENTATION_CONFIGURATION_NAME, annotationDependency)
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
