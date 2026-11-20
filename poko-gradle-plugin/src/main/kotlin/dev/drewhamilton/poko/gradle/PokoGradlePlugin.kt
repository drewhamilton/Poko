package dev.drewhamilton.poko.gradle

import dev.drewhamilton.poko.gradle.BuildConfig.DEFAULT_POKO_ANNOTATION
import dev.drewhamilton.poko.gradle.BuildConfig.DEFAULT_POKO_ENABLED
import dev.drewhamilton.poko.gradle.BuildConfig.DEFAULT_POKO_FIR_IDE_MODE
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.compiler.plugin.devkit.DevKitSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class PokoGradlePlugin : DevKitSupportPlugin(PluginInfo.PLUGIN_INFO) {

    override fun apply(target: Project) {
        val extension = target.extensions.create("poko", PokoPluginExtension::class.java)
        extension.enabled.convention(DEFAULT_POKO_ENABLED)
        extension.pokoAnnotation.convention(DEFAULT_POKO_ANNOTATION)
        extension.firIdeMode.convention(PokoFirIdeMode.valueOf(DEFAULT_POKO_FIR_IDE_MODE))
    }

    override fun Project.shouldAddRuntimeDependency(): Boolean =
        extensions.getByType(PokoPluginExtension::class.java).pokoAnnotation.get() == DEFAULT_POKO_ANNOTATION

    override fun Project.applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val extension = extensions.getByType(PokoPluginExtension::class.java)

        val optionsProvider = objects.listProperty(SubpluginOption::class.java)
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
        optionsProvider.add(
            extension.firIdeMode.map {
                SubpluginOption(
                    key = BuildConfig.POKO_FIR_IDE_MODE_OPTION_NAME,
                    value = it.name,
                )
            }
        )

        return optionsProvider
    }
}
