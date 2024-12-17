package dev.drewhamilton.poko

import com.google.auto.service.AutoService
import dev.drewhamilton.poko.BuildConfig.DEFAULT_POKO_ANNOTATION
import dev.drewhamilton.poko.BuildConfig.DEFAULT_POKO_ENABLED
import dev.drewhamilton.poko.fir.PokoFirExtensionRegistrar
import dev.drewhamilton.poko.ir.GeneratedDeclarationsIrBodyFiller
import dev.drewhamilton.poko.ir.PokoIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
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

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (!configuration.get(CompilerOptions.ENABLED, DEFAULT_POKO_ENABLED))
            return

        val pokoAnnotationString = configuration.get(CompilerOptions.POKO_ANNOTATION, DEFAULT_POKO_ANNOTATION)
        val pokoAnnotationClassId = ClassId.fromString(pokoAnnotationString)
        val messageCollector = configuration.get(
            CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE,
        )

        IrGenerationExtension.registerExtension(
            PokoIrGenerationExtension(pokoAnnotationClassId, messageCollector)
        )
        // TODO: Uncomment when ready
//        IrGenerationExtension.registerExtension(GeneratedDeclarationsIrBodyFiller())

        FirExtensionRegistrarAdapter.registerExtension(
            PokoFirExtensionRegistrar(pokoAnnotationClassId)
        )
    }
}
