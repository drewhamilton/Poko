package dev.drewhamilton.extracare.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.primaryConstructor

internal class DataApiMembersGenerator(
    private val pluginContext: IrPluginContext,
    private val annotationClass: IrClassSymbol,
    private val messageCollector: MessageCollector,
) : IrElementTransformerVoidWithContext() {
    override fun visitClassNew(declaration: IrClass): IrStatement {
        log("Reading <$declaration>")

        fun callSuper() = super.visitClassNew(declaration)

        if (!declaration.hasAnnotation(annotationClass)) {
            log("Not @DataApi")
            return callSuper()
        } else if (declaration.isData) {
            log("Data class")
            reportError("@DataApi does not support data classes")
            return callSuper()
        }

        val primaryConstructor = declaration.primaryConstructor
        if (primaryConstructor == null) {
            log("No primary constructor")
            reportError("@DataApi classes must have a primary constructor")
            return callSuper()
        }

        primaryConstructor.valueParameters

        return callSuper()
    }

    private fun log(message: String) {
        messageCollector.report(CompilerMessageSeverity.LOGGING, "EXTRA CARE COMPILER PLUGIN (IR): $message")
    }

    // TODO: Mandatory location
    @Deprecated("Provide a location when errors occur")
    private fun reportError(message: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, "EXTRA CARE COMPILER PLUGIN (IR): $message")
    }

    private fun reportError(message: String, location: CompilerMessageSourceLocation) {
        messageCollector.report(CompilerMessageSeverity.ERROR, "EXTRA CARE COMPILER PLUGIN (IR): $message", location)
    }
}
