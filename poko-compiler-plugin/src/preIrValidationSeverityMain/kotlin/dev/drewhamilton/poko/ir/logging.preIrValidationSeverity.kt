package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

internal actual fun Reporter(
    configuration: CompilerConfiguration,
    context: IrPluginContext
): Reporter = object : Reporter {
    private val messageCollector = configuration[CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE]

    override fun reportError(message: String, location: CompilerMessageSourceLocation?) =
        messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
}
