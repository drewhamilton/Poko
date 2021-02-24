package dev.drewhamilton.poko

import com.google.auto.service.AutoService
import dev.drewhamilton.poko.codegen.PokoCodegenExtension
import dev.drewhamilton.poko.ir.PokoIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName

@AutoService(ComponentRegistrar::class)
class PokoComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        if (configuration[CompilerOptions.ENABLED] != true)
            return

        val pokoAnnotationName = FqName(checkNotNull(configuration[CompilerOptions.POKO_ANNOTATION]))
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        // TODO: Use ClassBuilderInterceptorExtension, ExpressionCodegenExtension, or both?
        ExpressionCodegenExtension.registerExtension(
            project,
            PokoCodegenExtension(pokoAnnotationName, messageCollector)
        )
        IrGenerationExtension.registerExtension(
            project,
            PokoIrGenerationExtension(pokoAnnotationName, messageCollector)
        )
    }
}
