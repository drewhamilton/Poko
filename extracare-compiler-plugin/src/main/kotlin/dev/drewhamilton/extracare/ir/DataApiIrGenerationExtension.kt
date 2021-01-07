package dev.drewhamilton.extracare.ir

import dev.drewhamilton.extracare.dataApiAnnotationName
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class DataApiIrGenerationExtension(
    private val messageCollector: MessageCollector
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val dataApiAnnotation = pluginContext.referenceClass(dataApiAnnotationName)!!
        val dataApiMembersTransformer = DataApiMembersTransformer(pluginContext, dataApiAnnotation, messageCollector)
        moduleFragment.transform(dataApiMembersTransformer, null)
    }
}
