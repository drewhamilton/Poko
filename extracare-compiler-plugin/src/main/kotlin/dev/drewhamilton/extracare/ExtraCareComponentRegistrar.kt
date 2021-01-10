package dev.drewhamilton.extracare

import com.google.auto.service.AutoService
import dev.drewhamilton.extracare.codegen.DataApiCodegenExtension
import dev.drewhamilton.extracare.ir.DataApiIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.impl.ExtensionPointImpl
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

@AutoService(ComponentRegistrar::class)
class ExtraCareComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        if (configuration[CompilerOptions.ENABLED] == false)
            return

        // TODO: Use ClassBuilderInterceptorExtension, ExpressionCodegenExtension, or both?

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        ExpressionCodegenExtension.registerExtensionAsFirst(project, DataApiCodegenExtension(messageCollector))
        IrGenerationExtension.registerExtensionAsFirst(project, DataApiIrGenerationExtension(messageCollector))
    }

    private fun <T : Any> ProjectExtensionDescriptor<T>.registerExtensionAsFirst(project: Project, extension: T) {
        project.extensionArea
            .getExtensionPoint(extensionPointName)
            .let { it as ExtensionPointImpl }
            .registerExtension(extension, project)
    }
}
