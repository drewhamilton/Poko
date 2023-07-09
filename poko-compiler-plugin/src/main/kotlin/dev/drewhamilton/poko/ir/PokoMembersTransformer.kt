package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.getValueArgument
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.isMultiFieldValueClass
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.source.getPsi

internal class PokoMembersTransformer(
    private val pokoAnnotationName: ClassId,
    private val pluginContext: IrPluginContext,
    private val messageCollector: MessageCollector,
) : IrElementTransformerVoidWithContext() {

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        messageCollector.log("Reading <$declaration>")

        val declarationParent = declaration.parent
        if (declarationParent !is IrClass || !declaration.isFakeOverride) return declaration
        val pokoData = declarationParent.pokoData()
        if (pokoData != null) {
            with(pluginContext) {
                when {
                    declaration.isEquals() -> declaration.convertToGenerated(pokoData) { properties ->
                        generateEqualsMethodBody(
                            irClass = declarationParent,
                            functionDeclaration = declaration,
                            classProperties = properties,
                            messageCollector = messageCollector,
                        )
                    }

                    declaration.isHashCode() -> declaration.convertToGenerated(pokoData) { properties ->
                        generateHashCodeMethodBody(
                            functionDeclaration = declaration,
                            classProperties = properties,
                            messageCollector = messageCollector,
                        )
                    }

                    declaration.isToString() -> declaration.convertToGenerated(pokoData) { properties ->
                        generateToStringMethodBody(
                            irClass = declarationParent,
                            functionDeclaration = declaration,
                            classProperties = properties,
                            messageCollector = messageCollector,
                        )
                    }
                }
            }
        }

        return super.visitFunctionNew(declaration)
    }

    /**
     * If a poko annotation is found, validates it and returns associated [PokoData] for it.
     *
     * If this returns null, no poko annotation was found.
     */
    private fun IrClass.pokoData(): PokoData? {
        val annotation = getAnnotation(pokoAnnotationName.asSingleFqName())
        return when {
            annotation == null -> {
                messageCollector.log("Not Poko class")
                null
            }
            isData -> {
                messageCollector.log("Data class")
                messageCollector.reportErrorOnClass(this, "Poko does not support data classes")
                null
            }
            isSingleFieldValueClass || isMultiFieldValueClass -> {
                messageCollector.log("Value class")
                messageCollector.reportErrorOnClass(this, "Poko does not support value classes")
                null
            }
            isInner -> {
                messageCollector.log("Inner class")
                messageCollector.reportErrorOnClass(this, "Poko cannot be applied to inner classes")
                null
            }
            primaryConstructor == null -> {
                messageCollector.log("No primary constructor")
                messageCollector.reportErrorOnClass(
                    irClass = this,
                    message = "Poko classes must have a primary constructor",
                )
                null
            }
            else -> {
                val excludeFunctionsArg = annotation.getValueArgument(Name.identifier("excludeFunctions"))
                val excludeFunctions = if (excludeFunctionsArg != null) {
                    if (excludeFunctionsArg !is IrConst<*> || excludeFunctionsArg.kind !is IrConstKind.Boolean) {
                        messageCollector.log("Wrong type for excludeFunctions")
                        messageCollector.reportErrorOnClass(
                            irClass = this,
                            message = "excludeFunctions property on Poko annotations must be a Boolean type",
                        )
                        return null
                    }
                    @Suppress("UNCHECKED_CAST")
                    (excludeFunctionsArg as IrConst<Boolean>).value
                } else {
                    false
                }
                PokoData(excludeFunctions)
            }
        }
    }

    private inline fun IrFunction.convertToGenerated(
        pokoData: PokoData,
        generateFunctionBody: IrBlockBodyBuilder.(List<IrProperty>) -> Unit
    ) {
        val parent = parent as IrClass
        val properties = parent.properties
            .filter {
                // Filter out function properties if requested
                !(pokoData.excludeFunctions && it.type.isFunction())
            }
            .filter {
                // Can't figure out how to check this another way. FIR?
                @OptIn(ObsoleteDescriptorBasedAPI::class)
                it.symbol.descriptor.source.getPsi() is KtParameter
            }
            .toList()
        if (properties.isEmpty()) {
            messageCollector.log("No primary constructor properties")
            messageCollector.reportErrorOnClass(
                irClass = parent,
                message = "Poko classes must have at least one property in the primary constructor",
            )
            return
        }

        origin = PokoOrigin
        mutateWithNewDispatchReceiverParameterForParentClass()

        body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
            generateFunctionBody(properties)
        }

        reflectivelySetFakeOverride(false)
    }

    /**
     * Converts the function's dispatch receiver parameter (i.e. <this>) to the function's parent.
     * This is necessary because we are taking the base declaration from a parent class (or Any) and
     * pseudo-overriding it in this function's parent class.
     */
    private fun IrFunction.mutateWithNewDispatchReceiverParameterForParentClass() {
        val parentClass = parent as IrClass
        val originalReceiver = requireNotNull(dispatchReceiverParameter)
        dispatchReceiverParameter = IrValueParameterImpl(
            startOffset = originalReceiver.startOffset,
            endOffset = originalReceiver.endOffset,
            origin = originalReceiver.origin,
            symbol = IrValueParameterSymbolImpl(
                // IrValueParameterSymbolImpl requires a descriptor; same type as
                // originalReceiver.symbol:
                @OptIn(ObsoleteDescriptorBasedAPI::class)
                LazyClassReceiverParameterDescriptor(parentClass.descriptor),
            ),
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

    /**
     * Uses reflection to set an [IrFunction]'s private `isFakeOverride` property.
     */
    private fun IrFunction.reflectivelySetFakeOverride(isFakeOverride: Boolean) {
        with(javaClass.getDeclaredField("isFakeOverride")) {
            isAccessible = true
            setBoolean(this@reflectivelySetFakeOverride, isFakeOverride)
        }
    }
}
