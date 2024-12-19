package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType

/**
 * Alias for [irCall] from 2.1.0 – 2.1.20.
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
        // 2.1.0:
        irCall(
            callee = callee,
            type = type,
            valueArgumentsCount = valueArgumentsCount,
            typeArgumentsCount = typeArgumentsCount,
            origin = origin,
        )
    } catch (noSuchMethodError: NoSuchMethodError) {
        // TODO: Flip order when 2.1.20 is the compile version
        // https://github.com/JetBrains/kotlin/blob/v2.1.20-Beta1/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/builders/ExpressionHelpers.kt#L240
        javaClass.classLoader.loadClass("org.jetbrains.kotlin.ir.builders.ExpressionHelpersKt")
            .methods
            .single { function ->
                function.name == "irCall" &&
                    function.parameters.map { it.type } == listOf(
                        IrBuilderWithScope::class.java, // extension receiver
                        IrSimpleFunctionSymbol::class.java, // callee
                        IrType::class.java, // type
                        Int::class.java, // typeArgumentsCount
                        IrStatementOrigin::class.java, // origin
                    )
            }
            .invoke(
                null, // static invocation
                this, // extension receiver
                callee, // param: callee
                type, // param: type
                typeArgumentsCount, // param: type
                origin, // param: origin
            ) as IrCall
    }
}
