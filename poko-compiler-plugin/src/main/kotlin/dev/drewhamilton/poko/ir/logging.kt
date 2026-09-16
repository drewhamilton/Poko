package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.validation.IrValidationSeverity
import org.jetbrains.kotlin.resolve.source.getPsi

internal fun IrDiagnosticReporter.reportErrorOnClass(irClass: IrClass, message: String) {
    val psi = irClass.source.getPsi()
    val location = MessageUtil.psiElementToMessageLocation(psi)
    report(IrValidationSeverity.ERROR.factory, message, location)
}

@OptIn(ObsoleteDescriptorBasedAPI::class) // TODO: Try to do this without the descriptor
internal fun IrDiagnosticReporter.reportErrorOnProperty(property: IrProperty, message: String) {
    val psi = property.descriptor.source.getPsi()
    val location = MessageUtil.psiElementToMessageLocation(psi)
    report(IrValidationSeverity.ERROR.factory, message, location)
}
