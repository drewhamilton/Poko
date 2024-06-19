package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.ClassId
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
        if (declarationParent is IrClass && declarationParent.isPokoClass() && declaration.isFakeOverride) {
            with(pluginContext) {
                when {
                    declaration.isEquals() -> declaration.convertToGenerated { properties ->
                        generateEqualsMethodBody(
                            irClass = declarationParent,
                            functionDeclaration = declaration,
                            classProperties = properties,
                            messageCollector = messageCollector,
                        )
                    }

                    declaration.isHashCode() -> declaration.convertToGenerated { properties ->
                        generateHashCodeMethodBody(
                            functionDeclaration = declaration,
                            classProperties = properties,
                            messageCollector = messageCollector,
                        )
                    }

                    declaration.isToString() -> declaration.convertToGenerated { properties ->
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

    private fun IrClass.isPokoClass(): Boolean = when {
        !hasAnnotation(pokoAnnotationName.asSingleFqName()) -> {
            messageCollector.log("Not Poko class")
            false
        }
        isData -> {
            messageCollector.log("Data class")
            messageCollector.reportErrorOnClass(this, "Poko cannot be applied to a data class")
            false
        }
        isValue -> {
            messageCollector.log("Value class")
            messageCollector.reportErrorOnClass(this, "Poko cannot be applied to a value class")
            false
        }
        isInner -> {
            messageCollector.log("Inner class")
            messageCollector.reportErrorOnClass(this, "Poko cannot be applied to an inner class")
            false
        }
        primaryConstructor == null -> {
            messageCollector.log("No primary constructor")
            messageCollector.reportErrorOnClass(
                irClass = this,
                message = "Poko class must have a primary constructor",
            )
            false
        }
        else -> {
            true
        }
    }

    private inline fun IrFunction.convertToGenerated(
        generateFunctionBody: IrBlockBodyBuilder.(List<IrProperty>) -> Unit
    ) {
        val parent = parent as IrClass
        val properties = parent.properties
            .toList()
            .filter {
                val metadata = it.metadata
                if (metadata is FirMetadataSource.Property) {
                    // Using K2:
                    metadata.fir.source?.kind is KtFakeSourceElementKind.PropertyFromParameter
                } else {
                    // Not using K2:
                    @OptIn(ObsoleteDescriptorBasedAPI::class)
                    it.symbol.descriptor.source.getPsi() is KtParameter
                }
            }
        if (properties.isEmpty()) {
            messageCollector.log("No primary constructor properties")
            messageCollector.reportErrorOnClass(
                irClass = parent,
                message = "Poko class primary constructor must have at least one property",
            )
            return
        }

        origin = PokoOrigin
        mutateWithNewDispatchReceiverParameterForParentClass()

        body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
            generateFunctionBody(properties)
        }

        (this as IrSimpleFunction).apply {
            isFakeOverride = false
            isExternal = false
        }
    }

    /**
     * Converts the function's dispatch receiver parameter (i.e. <this>) to the function's parent.
     * This is necessary because we are taking the base declaration from a parent class (or Any) and
     * pseudo-overriding it in this function's parent class.
     */
    private fun IrFunction.mutateWithNewDispatchReceiverParameterForParentClass() {
        val parentClass = parent as IrClass
        val originalReceiver = requireNotNull(dispatchReceiverParameter)
        dispatchReceiverParameter = IrFactoryImpl.createValueParameter(
            startOffset = originalReceiver.startOffset,
            endOffset = originalReceiver.endOffset,
            origin = originalReceiver.origin,
            symbol = IrValueParameterSymbolImplCompat(
                // IrValueParameterSymbolImpl requires a descriptor; same type as
                // originalReceiver.symbol:
                descriptor = LazyClassReceiverParameterDescriptor(
                    @OptIn(ObsoleteDescriptorBasedAPI::class) parentClass.descriptor,
                ),
                signature = parentClass.symbol.signature,
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
     * Instantiate an [IrValueParameterSymbolImpl] via reflection if the known constructor is not
     * available. Provides forward compatibility with 2.0.20, which changes the constructor's
     * signature.
     */
    @Suppress("FunctionName") // Factory
    private fun IrValueParameterSymbolImplCompat(
        descriptor: ParameterDescriptor,
        signature: IdSignature?,
    ): IrValueParameterSymbolImpl {
        try {
            // Constructor available in 2.0.20+:
            return IrValueParameterSymbolImpl(descriptor, signature)
        } catch (noSuchMethodError: NoSuchMethodError) {
            // Old constructor pre-2.0.20:
            val implClass = IrValueParameterSymbolImpl::class.java
            val newConstructor = implClass.constructors.single {
                it.parameters.single().type == ParameterDescriptor::class.java
            }
            return newConstructor.newInstance(
                descriptor, // param: descriptor
            ) as IrValueParameterSymbolImpl
        }
    }
}
