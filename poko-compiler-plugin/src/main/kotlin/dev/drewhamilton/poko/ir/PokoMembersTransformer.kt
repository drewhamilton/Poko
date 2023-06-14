package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.isMultiFieldValueClass
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
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
                        generateToStringMethodBody(declarationParent, declaration, properties)
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
            messageCollector.reportErrorOnClass(this, "Poko does not support data classes")
            false
        }
        isSingleFieldValueClass || isMultiFieldValueClass -> {
            messageCollector.log("Value class")
            messageCollector.reportErrorOnClass(this, "Poko does not support value classes")
            false
        }
        isInner -> {
            messageCollector.log("Inner class")
            messageCollector.reportErrorOnClass(this, "Poko cannot be applied to inner classes")
            false
        }
        primaryConstructor == null -> {
            messageCollector.log("No primary constructor")
            messageCollector.reportErrorOnClass(
                irClass = this,
                message = "Poko classes must have a primary constructor",
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
                // Can't figure out how to check this another way. FIR?
                @OptIn(ObsoleteDescriptorBasedAPI::class)
                it.symbol.descriptor.source.getPsi() is KtParameter
            }
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

    //region toString
    private fun IrFunction.isToString(): Boolean =
        name == Name.identifier("toString") &&
                returnType == pluginContext.irBuiltIns.stringType &&
                valueParameters.isEmpty()

    /**
     * Generate the body of the toString method. Adapted from
     * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateToStringMethodBody].
     */
    private fun IrBlockBodyBuilder.generateToStringMethodBody(
        irClass: IrClass,
        irFunction: IrFunction,
        irProperties: List<IrProperty>,
    ) {
        val irConcat = irConcat()
        irConcat.addArgument(irString(irClass.name.asString() + "("))
        var first = true
        for (property in irProperties) {
            if (!first) irConcat.addArgument(irString(", "))

            irConcat.addArgument(irString(property.name.asString() + "="))

            val irPropertyValue = irGetField(irFunction.receiver(), property.backingField!!)

            val classifier = property.type.classifierOrNull
            val hasArrayContentBasedAnnotation =
                property.hasAnnotation(ArrayContentBasedAnnotation.asSingleFqName())
            // TODO: Handle generic type property that is array at runtime
            val mayBeRuntimeArray = classifier == context.irBuiltIns.anyClass
            val irPropertyStringValue = when {
                hasArrayContentBasedAnnotation && mayBeRuntimeArray -> {
                    val field = property.backingField!!
                    val instance = irGetField(irFunction.receiver(), field)
                    irRuntimeArrayContentDeepToString(instance)
                }

                hasArrayContentBasedAnnotation -> {
                    val toStringFunctionSymbol = maybeFindArrayDeepToStringFunction(property)
                        ?: context.irBuiltIns.dataClassArrayMemberToStringSymbol
                    irCall(
                        callee = toStringFunctionSymbol,
                        type = context.irBuiltIns.stringType,
                    ).apply {
                        // Poko modification: check for extension receiver for contentDeepToString
                        val hasExtensionReceiver =
                            toStringFunctionSymbol.owner.extensionReceiverParameter != null
                        if (hasExtensionReceiver) {
                            extensionReceiver = irPropertyValue
                        } else {
                            putValueArgument(0, irPropertyValue)
                        }
                    }
                }

                classifier.isArrayOrPrimitiveArray(context) -> {
                    irCall(
                        callee = context.irBuiltIns.dataClassArrayMemberToStringSymbol,
                        type = context.irBuiltIns.stringType
                    ).apply {
                        putValueArgument(0, irPropertyValue)
                    }
                }

                else -> irPropertyValue
            }

            irConcat.addArgument(irPropertyStringValue)
            first = false
        }
        irConcat.addArgument(irString(")"))
        +irReturn(irConcat)
    }

    /**
     * Returns `contentDeepToString` function symbol if it is an appropriate option for
     * [irProperty], else returns null.
     */
    private fun IrBlockBodyBuilder.maybeFindArrayDeepToStringFunction(
        irProperty: IrProperty,
    ): IrSimpleFunctionSymbol? {
        val propertyClassifier = irProperty.type.classifierOrFail

        if (!propertyClassifier.isArrayOrPrimitiveArray(context)) {
            messageCollector.reportErrorOnProperty(
                property = irProperty,
                message = "@ArrayContentBased on property of type <${irProperty.type.render()}> not supported",
            )
            return null
        }

        // Primitive arrays don't need deep toString:
        if (propertyClassifier in context.irBuiltIns.primitiveArraysToPrimitiveTypes) {
            return null
        }

        return findContentDeepToStringFunctionSymbol(propertyClassifier)
    }

    /**
     * Invokes a `when` branch that checks the runtime type of the [property] instance and invokes
     * `contentDeepToString` or `contentToString` for typed arrays and primitive arrays,
     * respectively.
     */
    private fun IrBlockBodyBuilder.irRuntimeArrayContentDeepToString(
        property: IrExpression,
    ): IrExpression {
        val starArrayType = context.irBuiltIns.arrayClass.createType(
            hasQuestionMark = false,
            arguments = listOf(IrStarProjectionImpl),
        )
        return irWhen(
            type = context.irBuiltIns.stringType,
            branches = listOf(
                irBranch(
                    condition = irIs(
                        argument = property,
                        type = starArrayType,
                    ),
                    result = irCall(
                        callee = findContentDeepToStringFunctionSymbol(
                            context.irBuiltIns.arrayClass,
                        ),
                        type = context.irBuiltIns.stringType,
                    ).apply {
                        extensionReceiver = property
                    }
                ),

                // TODO: Primitive arrays

                irElseBranch(
                    irCall(
                        callee = context.irBuiltIns.extensionToString,
                        type = context.irBuiltIns.stringType,
                    ).apply {
                        extensionReceiver = property
                    }
                ),
            ),
        )
    }

    /**
     * Finds `contentDeepToString` function if [propertyClassifier] is a typed array, or
     * `contentToString` function if it is a primitive array.
     */
    private fun IrBuilderWithScope.findContentDeepToStringFunctionSymbol(
        propertyClassifier: IrClassifierSymbol,
    ): IrSimpleFunctionSymbol {
        val callableName = if (propertyClassifier == context.irBuiltIns.arrayClass) {
            "contentDeepToString"
        } else {
            "contentToString"
        }
        return pluginContext.referenceFunctions(
            callableId = CallableId(
                packageName = FqName("kotlin.collections"),
                callableName = Name.identifier(callableName),
            ),
        ).single { functionSymbol ->
            // Find the single function with the relevant array type and disambiguate against the
            // older non-nullable receiver overload:
            functionSymbol.owner.extensionReceiverParameter?.type?.let {
                it.classifierOrNull == propertyClassifier && it.isNullable()
            } ?: false
        }
    }
    //endregion
}
