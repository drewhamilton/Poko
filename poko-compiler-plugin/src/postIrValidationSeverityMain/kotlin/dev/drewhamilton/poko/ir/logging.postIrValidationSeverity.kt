package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.validation.IrValidationSeverity

internal actual fun Reporter(
    configuration: CompilerConfiguration,
    context: IrPluginContext,
): Reporter = object : Reporter {
    override fun reportError(message: String, location: CompilerMessageSourceLocation?) {
        context.diagnosticReporter.report(IrValidationSeverity.ERROR.factory, message, location)
    }
}
