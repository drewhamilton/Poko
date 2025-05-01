package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isEquals
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isHashCode
import org.jetbrains.kotlin.ir.util.isOverridable
import org.jetbrains.kotlin.ir.util.isToString
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * For use only with the K1 compiler. K2 uses [PokoFunctionBodyFiller].
 */
internal class PokoMembersTransformer(
    private val pokoAnnotationName: ClassId,
    private val pluginContext: IrPluginContext,
    private val messageCollector: MessageCollector,
) : IrElementTransformerVoidWithContext() {

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        messageCollector.log("Reading <$declaration>")

        val declarationParent = declaration.parent
        if (
            declarationParent is IrClass &&
            declarationParent.isPokoClass() &&
            declaration.isFakeOverride &&
            canOverride(declaration)
        ) {
            when {
                declaration.isEquals() -> declaration.convertToGenerated { properties ->
                    generateEqualsMethodBody(
                        pokoAnnotation = pokoAnnotationName,
                        context = pluginContext,
                        irClass = declarationParent,
                        functionDeclaration = declaration,
                        classProperties = properties,
                        messageCollector = messageCollector,
                    )
                }

                declaration.isHashCode() -> declaration.convertToGenerated { properties ->
                    generateHashCodeMethodBody(
                        pokoAnnotation = pokoAnnotationName,
                        context = pluginContext,
                        functionDeclaration = declaration,
                        classProperties = properties,
                        messageCollector = messageCollector,
                    )
                }

                declaration.isToString() -> declaration.convertToGenerated { properties ->
                    generateToStringMethodBody(
                        pokoAnnotation = pokoAnnotationName,
                        context = pluginContext,
                        irClass = declarationParent,
                        functionDeclaration = declaration,
                        classProperties = properties,
                        messageCollector = messageCollector,
                    )
                }
            }
        }

        return super.visitFunctionNew(declaration)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
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

    private fun canOverride(function: IrFunction): Boolean {
        val superclassSameFunction = function.parentAsClass.findNearestSuperclassFunction(
            name = function.name,
            parameters = function.parametersCompat
        )

        return superclassSameFunction?.isOverridable ?: true
    }

    /**
     * Recursively finds the function in this class's nearest superclass with the same signature.
     * Ignores super-interfaces.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.findNearestSuperclassFunction(
        name: Name,
        parameters: List<IrValueParameter>,
    ): IrFunction? {
        val superclass = superTypes
            .mapNotNull { it.getClass() }
            .filter { it.kind == ClassKind.CLASS }
            .apply { check(size < 2) { "Found multiple superclasses" } }
            .singleOrNull()
            ?: return null

        val superclassFunction = superclass.declarations
            .filterIsInstance<IrFunction>()
            .filter { function ->
                function.name == name &&
                    function.parametersCompat.map { it.type } == parameters.map { it.type }
            }
            .apply { check(size < 2) { "Found multiple identical superclass functions" } }
            .singleOrNull()

        return superclassFunction ?: superclass.findNearestSuperclassFunction(
            name = name,
            parameters = parameters,
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private inline fun IrFunction.convertToGenerated(
        generateFunctionBody: IrBlockBodyBuilder.(List<IrProperty>) -> Unit
    ) {
        val parent = parent as IrClass
        val properties = parent.pokoProperties(pokoAnnotationName)
        if (properties.isEmpty()) {
            messageCollector.log("No primary constructor properties")
            messageCollector.reportErrorOnClass(
                irClass = parent,
                message = "Poko class primary constructor must have at least one not-skipped property",
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
            symbol = IrValueParameterSymbolImpl(
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
}
