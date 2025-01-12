package dev.drewhamilton.poko

import com.google.auto.service.AutoService
import dev.drewhamilton.poko.BuildConfig.DEFAULT_POKO_ANNOTATION
import dev.drewhamilton.poko.BuildConfig.DEFAULT_POKO_ENABLED
import dev.drewhamilton.poko.fir.PokoFirExtensionRegistrar
import dev.drewhamilton.poko.ir.PokoIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.ClassId

@ExperimentalCompilerApi
@AutoService(CompilerPluginRegistrar::class)
public class PokoCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean get() = true

    private val knownPokoPluginArgs = emptySet<String>()

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (!configuration.get(CompilerOptions.ENABLED, DEFAULT_POKO_ENABLED))
            return

        val pokoAnnotationString = configuration.get(CompilerOptions.POKO_ANNOTATION, DEFAULT_POKO_ANNOTATION)
        val pokoAnnotationClassId = ClassId.fromString(pokoAnnotationString)

        val messageCollector = configuration.get(
            CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE,
        )

        val pokoPluginArgs = configuration.get(CompilerOptions.POKO_PLUGIN_ARGS, emptyList())
            .associate { arg ->
                arg.split(BuildConfig.POKO_PLUGIN_ARGS_ITEM_DELIMITER).let {
                    it.first() to it.last()
                }
            }
        pokoPluginArgs.keys.forEach { pluginArgName ->
            if (!knownPokoPluginArgs.contains(pluginArgName)) {
                messageCollector.report(
                    severity = CompilerMessageSeverity.WARNING,
                    message = "Ignoring unknown Poko plugin arg: $pluginArgName",
                )
            }
        }

        IrGenerationExtension.registerExtension(
            PokoIrGenerationExtension(pokoAnnotationClassId, messageCollector)
        )

        FirExtensionRegistrarAdapter.registerExtension(
            PokoFirExtensionRegistrar(pokoAnnotationClassId)
        )
    }
}
