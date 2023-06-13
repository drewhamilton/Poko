package dev.drewhamilton.poko

import com.google.auto.service.AutoService
import dev.drewhamilton.poko.ir.PokoIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.ClassId

@ExperimentalCompilerApi
@AutoService(CompilerPluginRegistrar::class)
public class PokoCompilerPluginRegistrar : CompilerPluginRegistrar() {

    // TODO: Support K2
    override val supportsK2: Boolean = false

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[CompilerOptions.ENABLED] != true)
            return

        val pokoAnnotationString = checkNotNull(configuration[CompilerOptions.POKO_ANNOTATION])
        val pokoAnnotationClassId = ClassId.fromString(pokoAnnotationString)
        val messageCollector = configuration.get(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE,
        )

        IrGenerationExtension.registerExtension(
            PokoIrGenerationExtension(pokoAnnotationClassId, messageCollector)
        )
    }
}
