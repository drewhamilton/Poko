package dev.drewhamilton.careful

import dev.drewhamilton.careful.codegen.ToStringGenerator
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
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
            reportError("@Careful does not support data classes", codegen)
            return
        }

        val primaryConstructor = targetClass.constructors.firstOrNull { it.isPrimary }
        if (primaryConstructor == null) {
            log("No primary constructor")
            reportError("@Careful classes must have a primary constructor", codegen)
            return
        }

        val properties: List<PropertyDescriptor> = primaryConstructor.valueParameters.mapNotNull { parameter ->
            codegen.bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter)
        }


        ToStringGenerator(
            declaration = codegen.myClass as KtClassOrObject,
            classDescriptor = targetClass,
            classAsmType = codegen.typeMapper.mapType(targetClass),
            fieldOwnerContext = codegen.context,
            v = codegen.v,
            generationState = codegen.state
        ).generateToStringMethod(
            targetClass.findToStringFunction(),
            properties
        )
        // TODO("Generate equals")
        // TODO("Generate hashCode")
        // TODO("Generate Builder")
        // TODO("Generate top-level DSL constructor")
    }

    private fun ClassDescriptor.findToStringFunction(): SimpleFunctionDescriptor? =
        unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("toString"),
            NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS
        ).first()

    private fun log(message: String) {
        messageCollector.report(
            CompilerMessageSeverity.LOGGING,
            "CAREFUL COMPILER PLUGIN: $message",
            CompilerMessageLocation.create(null)
        )
    }

    private fun reportError(message: String, codegen: ImplementationBodyCodegen) {
        val psi = codegen.descriptor.source.getPsi()
        val location = MessageUtil.psiElementToMessageLocation(psi)
        messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
    }
}
