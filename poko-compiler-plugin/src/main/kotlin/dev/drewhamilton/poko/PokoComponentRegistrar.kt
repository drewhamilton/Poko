package dev.drewhamilton.poko

import com.google.auto.service.AutoService
import dev.drewhamilton.poko.ir.PokoIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.ClassId

// TODO: implement K2 support and switch to CompilerPluginRegistrar
@AutoService(ComponentRegistrar::class)
@FirIncompatiblePluginAPI
@ExperimentalCompilerApi
class PokoComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        if (configuration[CompilerOptions.ENABLED] != true)
            return

        val pokoAnnotationString = checkNotNull(configuration[CompilerOptions.POKO_ANNOTATION])
        val pokoAnnotationClassId = ClassId.fromString(pokoAnnotationString)
        val pokoAnnotationName = pokoAnnotationClassId.asSingleFqName()
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        IrGenerationExtension.registerExtension(
            project,
            PokoIrGenerationExtension(pokoAnnotationName, messageCollector)
        )
    }
}
