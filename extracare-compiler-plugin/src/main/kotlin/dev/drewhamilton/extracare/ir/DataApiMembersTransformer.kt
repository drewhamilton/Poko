package dev.drewhamilton.extracare.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
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
internal class DataApiMembersTransformer(
    private val pluginContext: IrPluginContext,
    private val annotationClass: IrClassSymbol,
    private val messageCollector: MessageCollector,
) : IrElementTransformerVoidWithContext() {

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        log("Reading <$declaration>")

        val declarationParent = declaration.parent
        if (declarationParent is IrClass && declarationParent.isDataApiClass() && declaration.isFakeOverride) {
            val properties = declarationParent.properties
                .toList()
                .filter { it.symbol.descriptor.source.getPsi() is KtParameter }
            if (declaration.isToString()) {
                declaration.convertToGeneratedToString(properties)
            }
        }

        return super.visitFunctionNew(declaration)
    }

    private fun IrClass.isDataApiClass(): Boolean {
        if (!hasAnnotation(annotationClass)) {
            log("Not @DataApi")
            return false
        } else if (isData) {
            log("Data class")
            reportError("@DataApi does not support data classes")
            return false
        } else if (isInline) {
            log("Inline class")
            reportError("@DataApi does not support inline classes")
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

    private fun IrFunction.isToString(): Boolean =
        name.asString() == "toString" && valueParameters.isEmpty() && returnType == pluginContext.irBuiltIns.stringType

    private fun IrFunction.convertToGeneratedToString(properties: List<IrProperty>) {
        val parent = parent as IrClass

        origin = DataApiOrigin

        mutateWithNewDispatchReceiverParameterForParentClass()

        body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
            +irReturn(generateToStringMethodBody(parent, this@convertToGeneratedToString, properties))
        }

        reflectivelySetFakeOverride(false)
    }

    private fun IrFunction.mutateWithNewDispatchReceiverParameterForParentClass() {
        val parentClass = parent
        require(parentClass is IrClass)
        val originalReceiver = checkNotNull(dispatchReceiverParameter)
        dispatchReceiverParameter = IrValueParameterImpl(
            startOffset = originalReceiver.startOffset,
            endOffset = originalReceiver.endOffset,
            origin = originalReceiver.origin,
            symbol = IrValueParameterSymbolImpl(LazyClassReceiverParameterDescriptor(parentClass.descriptor)),
            name = originalReceiver.name,
            index = originalReceiver.index,
            type = parentClass.symbol.createType(hasQuestionMark = false, emptyList()),
            varargElementType = originalReceiver.varargElementType,
            isCrossinline = originalReceiver.isCrossinline,
            isNoinline = originalReceiver.isNoinline,
            isHidden = originalReceiver.isHidden,
            isAssignable = originalReceiver.isAssignable
        ).apply {
            parent = this@mutateWithNewDispatchReceiverParameterForParentClass
        }
    }

    private fun IrFunction.reflectivelySetFakeOverride(isFakeOverride: Boolean) {
        with(javaClass.getDeclaredField("isFakeOverride")) {
            isAccessible = true
            setBoolean(this@reflectivelySetFakeOverride, isFakeOverride)
        }
    }

    /**
     * The actual body of the toString method. Copied from
     * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateToStringMethodBody].
     */
    private fun IrBuilderWithScope.generateToStringMethodBody(
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
            startOffset, endOffset,
            dispatchReceiverParameter.type,
            dispatchReceiverParameter.symbol
        )
    }

    private fun log(message: String) {
        messageCollector.report(CompilerMessageSeverity.LOGGING, "EXTRA CARE COMPILER PLUGIN (IR): $message")
    }

    private fun IrClass.reportError(message: String) {
        val location = CompilerMessageLocation.create(name.asString())
        messageCollector.report(CompilerMessageSeverity.ERROR, "EXTRA CARE COMPILER PLUGIN (IR): $message", location)
    }
}
