package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.types.isNullable as isNullableDeprecated
import org.jetbrains.kotlin.ir.types.superTypes as superTypesDeprecated

/**
 * Alias for [irCall] from 2.1.0 – 2.1.20.
 *
 * Remove when support for 2.1.0 & 2.1.1x is dropped.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrBuilderWithScope.irCallCompat(
    callee: IrSimpleFunctionSymbol,
    type: IrType,
    valueArgumentsCount: Int = callee.owner.valueParameters.size,
    typeArgumentsCount: Int = callee.owner.typeParameters.size,
    origin: IrStatementOrigin? = null,
): IrCall {
    return try {
        // 2.1.20+:
        irCall(
            callee = callee,
            type = type,
            typeArgumentsCount = typeArgumentsCount,
            origin = origin,
        )
    } catch (noSuchMethodError: NoSuchMethodError) {
        // https://github.com/JetBrains/kotlin/blob/v2.1.0/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/builders/ExpressionHelpers.kt#L240
        javaClass.classLoader.loadClass("org.jetbrains.kotlin.ir.builders.ExpressionHelpersKt")
            .methods
            .single { function ->
                function.name == "irCall" &&
                    function.parameters.map { it.type } == listOf(
                        IrBuilderWithScope::class.java, // extension receiver
                        IrSimpleFunctionSymbol::class.java, // callee
                        IrType::class.java, // type
                        Int::class.java, // valueArgumentsCount
                        Int::class.java, // typeArgumentsCount
                        IrStatementOrigin::class.java, // origin
                    )
            }
            .invoke(
                null, // static invocation
                this, // extension receiver
                callee, // param: callee
                type, // param: type
                valueArgumentsCount, // param: valueArgumentsCount
                typeArgumentsCount, // param: typeArgumentsCount
                origin, // param: origin
            ) as IrCall
    }
}

/**
 * Alias for [isNullable] from 2.1.0 – 2.1.20.
 *
 * Remove when support for 2.1.0 & 2.1.1x is dropped.
 */
internal fun IrType.isNullableCompat(): Boolean {
    return try {
        isNullable()
    } catch (noSuchMethodError: NoSuchMethodError) {
        @Suppress("DEPRECATION")
        isNullableDeprecated()
    }
}

/**
 * Alias for [superTypes] from 2.1.0 – 2.1.20.
 *
 * Remove when support for 2.1.0 & 2.1.1x is dropped.
 */
internal fun IrClassifierSymbol.superTypesCompat(): List<IrType> {
    return try {
        superTypes()
    } catch (noSuchMethodError: NoSuchMethodError) {
        @Suppress("DEPRECATION")
        superTypesDeprecated()
    }
}
