package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId

internal class PokoIrGenerationExtension(
    private val pokoAnnotationName: ClassId,
    private val reporter: (IrPluginContext) -> Reporter
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val reporter = reporter(pluginContext)
        if (pluginContext.referenceClass(pokoAnnotationName) == null) {
            reporter.reportError(message = "Could not find class <$pokoAnnotationName>")
            return
        }

        val bodyFiller = PokoFunctionBodyFiller(
            pokoAnnotation = pokoAnnotationName,
            context = pluginContext,
            reporter = reporter,
        )
        moduleFragment.acceptChildrenVoid(bodyFiller)
    }
}
