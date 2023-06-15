package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextInterface
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * The type of an [IrProperty].
 */
internal val IrProperty.type
    get() = backingField?.type
        ?: getter?.returnType
        ?: error("Can't find type of ${render()}")

/**
 * The receiver value (i.e. `this`) for a function with a dispatch (i.e. non-extension) receiver.
 *
 * In the context of Poko, only works properly after the overridden method has had its
 * `dispatchReceiverParameter` updated to the current parent class.
 */
context(IrBlockBodyBuilder)
internal fun IrFunction.receiver(): IrGetValue = IrGetValueImpl(dispatchReceiverParameter!!)

/**
 * Gets the value of the given [parameter].
 */
context(IrBlockBodyBuilder)
internal fun IrGetValueImpl(
    parameter: IrValueParameter,
) = IrGetValueImpl(
    startOffset = startOffset,
    endOffset = endOffset,
    type = parameter.type,
    symbol = parameter.symbol,
)

internal fun IrProperty.hasArrayContentBasedAnnotation(): Boolean =
    hasAnnotation(arrayContentBasedAnnotationFqName)

private val arrayContentBasedAnnotationFqName = ClassId(
    FqName("dev.drewhamilton.poko"),
    Name.identifier("ArrayContentBased"),
).asSingleFqName()

/**
 * Returns true if the classifier represents a typed or primitive array.
 */
internal fun IrClassifierSymbol?.isArrayOrPrimitiveArray(
    context: IrGeneratorContext,
): Boolean {
    return this == context.irBuiltIns.arrayClass ||
        this in context.irBuiltIns.primitiveArraysToPrimitiveTypes
}

/**
 * Returns true if the classifier represents a type that may be an array at runtime (e.g. [Any] or
 * a generic type).
 */
// TODO: Handle generic type
internal fun IrClassifierSymbol?.mayBeRuntimeArray(
    context: IrGeneratorContext,
): Boolean {
    return this == context.irBuiltIns.anyClass
}

context(IrGeneratorContextInterface)
internal fun PrimitiveType.toPrimitiveArrayClassSymbol(): IrClassSymbol {
    return irBuiltIns.primitiveTypesToPrimitiveArrays.getValue(this)
}

context(IrGeneratorContextInterface)
internal fun IrClassSymbol.createArrayType(): IrSimpleType {
    val typeArguments = when {
        this == irBuiltIns.arrayClass -> listOf(IrStarProjectionImpl)
        this in irBuiltIns.primitiveArraysToPrimitiveTypes -> emptyList()
        else -> throw IllegalArgumentException("$this is not an array class symbol")
    }
    return createType(hasQuestionMark = false, arguments = typeArguments)
}
