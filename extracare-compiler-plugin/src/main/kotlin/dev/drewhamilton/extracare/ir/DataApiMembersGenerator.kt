package dev.drewhamilton.extracare.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.source.getPsi

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class DataApiMembersGenerator(
    private val pluginContext: IrPluginContext,
    private val annotationClass: IrClassSymbol,
    private val messageCollector: MessageCollector,
) : IrElementTransformerVoidWithContext() {

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        log("Reading <$declaration>")

        val declarationParent = declaration.parent
        if (declarationParent is IrClass && declaration.isFakeOverride && declarationParent.isDataApiClass()) {
            val properties = declarationParent.properties.toList().filter { it.symbol.descriptor.source.getPsi() is KtParameter }
            if (declaration.name.asString() == "toString" && declaration.valueParameters.isEmpty()) {
                declaration.origin = DataApiOrigin

                val initialReceiver = declaration.dispatchReceiverParameter!!
                declaration.dispatchReceiverParameter = IrValueParameterImpl(
                    startOffset = initialReceiver.startOffset,
                    endOffset = initialReceiver.endOffset,
                    origin = initialReceiver.origin,
                    symbol = IrValueParameterSymbolImpl(LazyClassReceiverParameterDescriptor(declarationParent.descriptor)),
                    name = initialReceiver.name,
                    index = initialReceiver.index,
                    type = declarationParent.symbol.createType(hasQuestionMark = false, emptyList()),
                    varargElementType = initialReceiver.varargElementType,
                    isCrossinline = initialReceiver.isCrossinline,
                    isNoinline = initialReceiver.isNoinline,
                    isHidden = initialReceiver.isHidden,
                    isAssignable = initialReceiver.isAssignable
                ).apply {
                    parent = declaration
                }

                declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).irBlockBody {
                    +irReturn(toString(declarationParent, declaration, properties))
                }
            }
        }

        return super.visitFunctionNew(declaration)
    }

    private fun IrDeclarationParent.isDataApiClass(): Boolean {
        if (this !is IrClass) {
            return false
        } else if (!hasAnnotation(annotationClass)) {
            log("Not @DataApi")
            return false
        } else if (isData) {
            log("Data class")
            reportError("@DataApi does not support data classes")
            return false
        }

        val primaryConstructor = primaryConstructor
        if (primaryConstructor == null) {
            log("No primary constructor")
            reportError("@DataApi classes must have a primary constructor")
            return false
        }

        return true
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

    private fun IrBuilderWithScope.toString(
        irClass: IrClass,
        irFunction: IrFunction,
        irProperties: List<IrProperty>,
    ): IrExpression {
        val irConcat = irConcat()
        irConcat.addArgument(irString(irClass.name.asString() + "("))
        var first = true
        for (property in irProperties) {
            if (!first) irConcat.addArgument(irString(", "))

            irConcat.addArgument(irString(property.name.asString() + "="))

            // FIXME: Not sure about irFunction.irThis()
            val irPropertyValue = irGetField(irFunction.irThis(), property.backingField!!)

            val typeConstructorDescriptor = property.descriptor.type.constructor.declarationDescriptor
            val irPropertyStringValue =
                if (
                    typeConstructorDescriptor is ClassDescriptor &&
                    KotlinBuiltIns.isArrayOrPrimitiveArray(typeConstructorDescriptor)
                )
                    irCall(context.irBuiltIns.dataClassArrayMemberToStringSymbol, context.irBuiltIns.stringType).apply {
                        putValueArgument(0, irPropertyValue)
                    }
                else
                    irPropertyValue

            irConcat.addArgument(irPropertyStringValue)
            first = false
        }
        irConcat.addArgument(irString(")"))
        return irConcat
    }

    private fun IrFunction.irThis(): IrExpression {
        val dispatchReceiverParameter = dispatchReceiverParameter!!
        return IrGetValueImpl(
            // TODO: Not sure about offsets
            startOffset, endOffset,
            dispatchReceiverParameter.type,
            dispatchReceiverParameter.symbol
        )
    }

    private object DataApiOrigin : IrDeclarationOriginImpl("GENERATED_DATA_API_CLASS_MEMBER")
}
