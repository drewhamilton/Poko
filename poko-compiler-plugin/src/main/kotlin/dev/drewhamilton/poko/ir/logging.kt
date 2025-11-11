package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.resolve.source.getPsi

internal fun MessageCollector.log(message: String) {
    report(CompilerMessageSeverity.LOGGING, "POKO COMPILER PLUGIN (IR): $message")
}

internal fun MessageCollector.reportErrorOnClass(irClass: IrClass, message: String) {
    val psi = irClass.source.getPsi()
    val location = MessageUtil.psiElementToMessageLocation(psi)
    report(CompilerMessageSeverity.ERROR, message, location)
}

@OptIn(ObsoleteDescriptorBasedAPI::class) // Only needed for non-K2 compilation
internal fun MessageCollector.reportErrorOnProperty(property: IrProperty, message: String) {
    val psi = property.descriptor.source.getPsi()
    val location = MessageUtil.psiElementToMessageLocation(psi)
    report(CompilerMessageSeverity.ERROR, message, location)
}
