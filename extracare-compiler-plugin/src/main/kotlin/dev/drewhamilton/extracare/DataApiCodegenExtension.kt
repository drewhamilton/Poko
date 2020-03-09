package dev.drewhamilton.extracare

import dev.drewhamilton.extracare.codegen.EqualsGenerator
import dev.drewhamilton.extracare.codegen.HashCodeGenerator
import dev.drewhamilton.extracare.codegen.ToStringGenerator
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

class DataApiCodegenExtension(
    private val messageCollector: MessageCollector
) : ExpressionCodegenExtension {

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        val targetClass = codegen.descriptor
        log("Reading ${targetClass.name}")

        if (!targetClass.isDataApi) {
            log("Not @DataApi")
            return
        } else if (targetClass.isData) {
            log("Data class")
            reportError("@DataApi does not support data classes", codegen)
            return
        }

        val primaryConstructor = targetClass.constructors.firstOrNull { it.isPrimary }
        if (primaryConstructor == null) {
            log("No primary constructor")
            reportError("@DataApi classes must have a primary constructor", codegen)
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
        ).generate(
            targetClass.findFunction("toString")!!,
            properties
        )

        EqualsGenerator(
            declaration = codegen.myClass as KtClassOrObject,
            classDescriptor = targetClass,
            classAsmType = codegen.typeMapper.mapType(targetClass),
            fieldOwnerContext = codegen.context,
            v = codegen.v,
            generationState = codegen.state
        ).generate(
            targetClass.findFunction("equals")!!,
            properties
        )

        HashCodeGenerator(
            declaration = codegen.myClass as KtClassOrObject,
            classDescriptor = targetClass,
            classAsmType = codegen.typeMapper.mapType(targetClass),
            fieldOwnerContext = codegen.context,
            v = codegen.v,
            generationState = codegen.state
        ).generate(
            targetClass.findFunction("hashCode")!!,
            properties
        )

        // TODO("Generate Builder")
        // TODO("Generate top-level DSL constructor")
    }

    private fun ClassDescriptor.findFunction(name: String): SimpleFunctionDescriptor? =
        unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier(name),
            NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS
        ).first()

    private fun log(message: String) {
        messageCollector.report(
            CompilerMessageSeverity.LOGGING,
            "EXTRA CARE COMPILER PLUGIN: $message",
            CompilerMessageLocation.create(null)
        )
    }

    private fun reportError(message: String, codegen: ImplementationBodyCodegen) {
        val psi = codegen.descriptor.source.getPsi()
        val location = MessageUtil.psiElementToMessageLocation(psi)
        messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
    }
}
