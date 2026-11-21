package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.resolve.source.getPsi

internal interface Reporter {
    fun reportError(message: String, location: CompilerMessageSourceLocation? = null)
}

internal expect fun Reporter(configuration: CompilerConfiguration, context: IrPluginContext): Reporter

context(reporter: Reporter)
internal fun reportErrorOnClass(irClass: IrClass, message: String) {
    val psi = irClass.source.getPsi()
    val location = MessageUtil.psiElementToMessageLocation(psi)
    reporter.reportError(message, location)
}

@OptIn(ObsoleteDescriptorBasedAPI::class) // TODO: Try to do this without the descriptor
context(reporter: Reporter)
internal fun reportErrorOnProperty(property: IrProperty, message: String) {
    val psi = property.descriptor.source.getPsi()
    val location = MessageUtil.psiElementToMessageLocation(psi)
    reporter.reportError(message, location)
}
