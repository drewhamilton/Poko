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

    // TODO: Update for 2.3.0
    @Suppress("unused") // Added for forward compatibility
    internal val pluginId: String get() = BuildConfig.COMPILER_PLUGIN_ARTIFACT

    override val supportsK2: Boolean get() = true

    private val firDeclarationGenerationPluginArg =
        "poko.experimental.enableFirDeclarationGeneration"
    private val knownPokoPluginArgs = setOf(
        firDeclarationGenerationPluginArg,
    )

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (!configuration.get(CompilerOptions.ENABLED, DEFAULT_POKO_ENABLED))
            return

        val pokoAnnotationString = configuration.get(CompilerOptions.POKO_ANNOTATION, DEFAULT_POKO_ANNOTATION)
        val pokoAnnotationClassId = ClassId.fromString(pokoAnnotationString)

        val messageCollector = configuration.get(
            CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE,
        )

        val pokoPluginArgs = configuration.get(CompilerOptions.POKO_PLUGIN_ARGS, emptyMap())
        pokoPluginArgs.keys.forEach { pluginArgName ->
            if (!knownPokoPluginArgs.contains(pluginArgName)) {
                messageCollector.report(
                    severity = CompilerMessageSeverity.WARNING,
                    message = "Ignoring unknown Poko plugin arg: $pluginArgName",
                )
            }
        }

        val firDeclarationGenerationPluginValue = pokoPluginArgs[firDeclarationGenerationPluginArg]
        val firDeclarationGenerationEnabled =
            firDeclarationGenerationPluginValue?.equals("false", ignoreCase = true)?.not()?.also {
                messageCollector.report(
                    severity = CompilerMessageSeverity.WARNING,
                    message = "<$firDeclarationGenerationPluginArg> resolved to $it. " +
                        "This experimental flag may disappear at any time.",
                )
            } ?: true

        IrGenerationExtension.registerExtension(
            PokoIrGenerationExtension(
                pokoAnnotationName = pokoAnnotationClassId,
                firDeclarationGeneration = firDeclarationGenerationEnabled,
                messageCollector = messageCollector,
            )
        )

        FirExtensionRegistrarAdapter.registerExtension(
            PokoFirExtensionRegistrar(
                pokoAnnotation = pokoAnnotationClassId,
                declarationGeneration = firDeclarationGenerationEnabled,
            )
        )
    }
}
