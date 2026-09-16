package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.validation.IrValidationSeverity
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId

internal class PokoIrGenerationExtension(
    private val pokoAnnotationName: ClassId,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        if (pluginContext.referenceClass(pokoAnnotationName) == null) {
            pluginContext.diagnosticReporter.report(
                factory = IrValidationSeverity.ERROR.factory,
                message = "Could not find class <$pokoAnnotationName>",
            )
            return
        }

        val bodyFiller = PokoFunctionBodyFiller(
            pokoAnnotation = pokoAnnotationName,
            context = pluginContext,
        )
        moduleFragment.acceptChildrenVoid(bodyFiller)
    }
}
