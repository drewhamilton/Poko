package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextInterface
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.superTypes
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isInterface
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
internal fun IrBlockBodyBuilder.receiver(
    function: IrFunction,
): IrGetValue = IrGetValueImpl(function.dispatchReceiverParameter!!)

//region Compat/reflection
/**
 * Gets the value of the given [parameter].
 *
 * Invoke [IrGetValueImpl] via reflection if the known function is not available. Provides forward
 * compatibility with 2.0.20, which changes the constructor's signature.
 */
// TODO: Revert to standard IrGetValueImpl when support for 2.0.10 is dropped
internal fun IrBlockBodyBuilder.IrGetValueImpl(
    parameter: IrValueParameter,
) = try {
    // Available in 2.0.20+:
    IrGetValueImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = parameter.type,
        symbol = parameter.symbol,
    )
} catch (noClassDefFoundError: NoClassDefFoundError) {
    // Old parent class pre-2.0.20:
    javaClass.classLoader.loadClass("org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImplKt")
        .methods
        .single { function ->
            function.name == "IrGetValueImpl" &&
                function.parameters.map { it.type } == listOf(
                    Int::class.java,
                    Int::class.java,
                    IrType::class.java,
                    IrValueSymbol::class.java,
                    IrStatementOrigin::class.java,
                )
        }
        .invoke(
            null, // static invocation
            startOffset, // param: startOffset
            endOffset, // param: endOffset
            parameter.type, // param: type
            parameter.symbol, // param: symbol
            null, // param: origin (default value is null)
        ) as IrGetValueImpl
}

// TODO: Remove when support for 2.0.10 is dropped
@Suppress("FunctionName") // Factory
internal fun Any.IrWhenImplCompat(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin? = null,
): IrWhenImpl {
    return try {
        IrWhenImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = type,
            origin = origin
        )
    } catch (noClassDefFoundError: NoClassDefFoundError) {
        javaClass.classLoader.loadClass("org.jetbrains.kotlin.ir.expressions.impl.IrWhenImplKt")
            .methods
            .single { function ->
                function.name == "IrWhenImpl" &&
                    function.parameters.map { it.type } == listOf(
                    Int::class.java,
                    Int::class.java,
                    IrType::class.java,
                    IrStatementOrigin::class.java,
                )
            }
            .invoke(
                null, // static invocation
                startOffset, // param: startOffset
                endOffset, // param: endOffset
                type, // param: type
                origin, // param: origin
            ) as IrWhenImpl
    }
}

// TODO: Remove when support for 2.0.10 is dropped
@Suppress("FunctionName") // Factory
internal fun Any.IrBranchImplCompat(
    startOffset: Int,
    endOffset: Int,
    condition: IrExpression,
    result: IrExpression,
): IrBranchImpl {
    return try {
        IrBranchImpl(startOffset, endOffset, condition, result)
    } catch (noClassDefFoundError: NoClassDefFoundError) {
        javaClass.classLoader.loadClass("org.jetbrains.kotlin.ir.expressions.impl.IrBranchImplKt")
            .methods
            .single { function ->
                function.name == "IrBranchImpl" &&
                    function.parameters.map { it.type } == listOf(
                    Int::class.java,
                    Int::class.java,
                    IrExpression::class.java,
                    IrExpression::class.java,
                )
            }
            .invoke(
                null, // static invocation
                startOffset, // param: startOffset
                endOffset, // param: endOffset
                condition, // param: condition
                result, // param: result
            ) as IrBranchImpl
    }
}
//endregion

internal fun IrProperty.hasArrayContentBasedAnnotation(): Boolean =
    hasAnnotation(arrayContentBasedAnnotationFqName)

private val arrayContentBasedAnnotationFqName = ClassId(
    FqName("dev.drewhamilton.poko"),
    Name.identifier("ArrayContentBased"),
).asSingleFqName()

/**
 * Returns true if the classifier represents a type that may be an array at runtime (e.g. [Any] or
 * a generic type).
 */
internal fun IrClassifierSymbol?.mayBeRuntimeArray(
    context: IrGeneratorContextInterface,
): Boolean {
    return this == context.irBuiltIns.anyClass ||
        (this is IrTypeParameterSymbol && hasArrayOrPrimitiveArrayUpperBound(context))
}

private fun IrTypeParameterSymbol.hasArrayOrPrimitiveArrayUpperBound(
    context: IrGeneratorContextInterface,
): Boolean {
    superTypes().forEach { superType ->
        val superTypeClassifier = superType.classifierOrNull
        // Note: A generic type cannot have an array as an upper bound, else that would also
        // be checked here.
        val foundUpperBoundMatch = superTypeClassifier == context.irBuiltIns.anyClass ||
            (superTypeClassifier is IrTypeParameterSymbol &&
                superTypeClassifier.hasArrayOrPrimitiveArrayUpperBound(context))

        if (foundUpperBoundMatch) {
            return true
        }
    }
    return false
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrTypeParameter.erasedUpperBound: IrClass
    get() {
        // Pick the (necessarily unique) non-interface upper bound if it exists
        for (type in superTypes) {
            val irClass = type.classOrNull?.owner ?: continue
            if (!irClass.isInterface && !irClass.isAnnotationClass) return irClass
        }

        // Otherwise, choose either the first IrClass supertype or recurse.
        // In the first case, all supertypes are interface types and the choice was arbitrary.
        // In the second case, there is only a single supertype.
        return when (val firstSuper = superTypes.first().classifierOrNull?.owner) {
            is IrClass -> firstSuper
            is IrTypeParameter -> firstSuper.erasedUpperBound
            else -> error("unknown supertype kind $firstSuper")
        }
    }

internal fun PrimitiveType.toPrimitiveArrayClassSymbol(
    context: IrGeneratorContextInterface,
): IrClassSymbol {
    return context.irBuiltIns.primitiveTypesToPrimitiveArrays.getValue(this)
}

internal fun IrClassSymbol.createArrayType(
    context: IrGeneratorContextInterface,
): IrSimpleType {
    val typeArguments = when {
        this == context.irBuiltIns.arrayClass -> listOf(IrStarProjectionImpl)
        this in context.irBuiltIns.primitiveArraysToPrimitiveTypes -> emptyList()
        else -> throw IllegalArgumentException("$this is not an array class symbol")
    }
    return createType(hasQuestionMark = false, arguments = typeArguments)
}
