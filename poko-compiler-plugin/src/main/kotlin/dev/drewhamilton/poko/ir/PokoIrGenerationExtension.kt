package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName

internal class PokoIrGenerationExtension(
    private val pokoAnnotationName: FqName,
    private val messageCollector: MessageCollector
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val pokoAnnotationClass = pluginContext.referenceClass(pokoAnnotationName)
        if (pokoAnnotationClass == null) {
            moduleFragment.reportError("Could not find class <$pokoAnnotationName>")
            return
        }

        val pokoMembersTransformer = PokoMembersTransformer(pokoAnnotationClass, pluginContext, messageCollector)
        moduleFragment.transform(pokoMembersTransformer, null)
    }

    private fun IrModuleFragment.reportError(message: String) {
        val psi = descriptor.findPsi()
        val location = MessageUtil.psiElementToMessageLocation(psi)
        messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
    }
}
