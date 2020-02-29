package dev.drewhamilton.careful

import dev.drewhamilton.careful.codegen.ToStringGenerator
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.source.getPsi

class CarefulCodegenExtension(
    private val messageCollector: MessageCollector
) : ExpressionCodegenExtension {

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        val targetClass = codegen.descriptor
        log("Reading ${targetClass.name}")

        if (!targetClass.isCareful) {
            log("Not @Careful")
            return
        } else if (targetClass.isData) {
            log("Data class")
            val psi = codegen.descriptor.source.getPsi()
            val location = MessageUtil.psiElementToMessageLocation(psi)
            messageCollector.report(
                CompilerMessageSeverity.ERROR, "@Careful does not support data classes",
                location
            )
            return
        }

        ToStringGenerator(
            declaration = codegen.myClass as KtClassOrObject,
            classDescriptor = targetClass,
            classAsmType = codegen.typeMapper.mapType(targetClass),
            fieldOwnerContext = codegen.context,
            v = codegen.v,
            generationState = codegen.state,
            replacementString = "TODO: Remove"
        )//.generateToStringMethod(
//            targetClass.findToStringFunction()!!,
//            properties
//        )
        TODO("Generate equals")
        TODO("Generate hashCode")
        TODO("Generate Builder")
        TODO("Generate top-level DSL constructor")
    }

    private fun log(message: String) {
        messageCollector.report(
            CompilerMessageSeverity.LOGGING,
            "CAREFUL COMPILER PLUGIN: $message",
            CompilerMessageLocation.create(null))
    }
}
