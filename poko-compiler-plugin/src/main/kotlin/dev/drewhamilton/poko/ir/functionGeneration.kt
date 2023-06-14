package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.util.render

/**
 * The type of an [IrProperty].
 */
internal val IrProperty.type
    get() = backingField?.type
        ?: getter?.returnType
        ?: error("Can't find type of ${render()}")

/**
 * Converts the function's dispatch receiver parameter (i.e. <this>) to the function's parent.
 * This is necessary because we are taking the base declaration from a parent class (or Any) and
 * pseudo-overriding it in this function's parent class.
 */
internal fun IrFunction.mutateWithNewDispatchReceiverParameterForParentClass() {
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
            LazyClassReceiverParameterDescriptor(parentClass.descriptor)
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
 * The receiver value (i.e. `this`) for a [function] with a dispatch (i.e. non-extension) receiver.
 *
 * In the context of Poko, only works properly after
 * [mutateWithNewDispatchReceiverParameterForParentClass] has been called on [function].
 */
internal fun IrBlockBodyBuilder.receiver(function: IrFunction): IrGetValue =
    IrGetValueImpl(function.dispatchReceiverParameter!!)

/**
 * Gets the value of the given [parameter].
 */
internal fun IrBlockBodyBuilder.IrGetValueImpl(
    parameter: IrValueParameter,
) = IrGetValueImpl(
    startOffset = startOffset,
    endOffset = endOffset,
    type = parameter.type,
    symbol = parameter.symbol,
)

/**
 * Uses reflection to set an [IrFunction]'s private `isFakeOverride` property.
 */
internal fun IrFunction.reflectivelySetFakeOverride(isFakeOverride: Boolean) {
    with(javaClass.getDeclaredField("isFakeOverride")) {
        isAccessible = true
        setBoolean(this@reflectivelySetFakeOverride, isFakeOverride)
    }
}

/**
 * Returns true if the classifier represents a typed or primitive array.
 */
internal fun IrClassifierSymbol?.isArrayOrPrimitiveArray(
    context: IrGeneratorContext,
): Boolean {
    return this == context.irBuiltIns.arrayClass ||
        this in context.irBuiltIns.primitiveArraysToPrimitiveTypes
}
