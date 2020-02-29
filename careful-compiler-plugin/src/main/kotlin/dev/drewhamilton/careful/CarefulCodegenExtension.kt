package dev.drewhamilton.careful

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.source.getPsi

class CarefulCodegenExtension(
    private val messageCollector: MessageCollector
) : ExpressionCodegenExtension {

    // TODO: Centralize
    private val carefulAnnotationName = FqName("dev.drewhamilton.careful.Careful")

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        val targetClass = codegen.descriptor
        log("Reading ${targetClass.name}")

        if (!targetClass.isCareful) {
            log("Not @Careful")
            return
        } else if (!targetClass.isData) {
            log("Not a data class")
            val psi = codegen.descriptor.source.getPsi()
            val location = MessageUtil.psiElementToMessageLocation(psi)
            messageCollector.report(
                CompilerMessageSeverity.ERROR, "@Careful is only supported on data classes",
                location
            )
            return
        }

        TODO("Actual generation")
    }

    private fun log(message: String) {
        messageCollector.report(
            CompilerMessageSeverity.LOGGING,
            "CAREFUL COMPILER PLUGIN: $message",
            CompilerMessageLocation.create(null))
    }

    private val Annotated.isCareful: Boolean
        get() = annotations.hasAnnotation(carefulAnnotationName)
}
