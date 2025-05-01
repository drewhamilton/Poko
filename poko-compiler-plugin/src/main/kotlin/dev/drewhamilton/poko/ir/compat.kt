package dev.drewhamilton.poko.ir

import java.lang.reflect.Method
import kotlin.reflect.KClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNotIs
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.types.isNullable as isNullableDeprecated
import org.jetbrains.kotlin.ir.types.superTypes as superTypesDeprecated

//region https://github.com/JetBrains/kotlin/blob/v2.2.0-Beta2/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/builders/ExpressionHelpers.kt
/**
 * Alias for [IrBuilderWithScope.irEquals] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irEqualsCompat(
    arg1: IrExpression,
    arg2: IrExpression,
    origin: IrStatementOrigin = IrStatementOrigin.EQEQ,
) = irExpressionHelperCompat(
    call = { irEquals(arg1, arg2, origin) },
    name = "irEquals",
    IrExpression::class to arg1,
    IrExpression::class to arg2,
    IrStatementOrigin::class to origin,
)

/**
 * Alias for [IrBuilderWithScope.irEqeqeq] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irEqeqeqCompat(
    arg1: IrExpression,
    arg2: IrExpression,
) = irExpressionHelperCompat(
    call = { irEqeqeq(arg1, arg2) },
    name = "irEqeqeq",
    IrExpression::class to arg1,
    IrExpression::class to arg2,
)

/**
 * Alias for [IrBuilderWithScope.irNotEquals] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irNotEqualsCompat(
    arg1: IrExpression,
    arg2: IrExpression,
) = irExpressionHelperCompat(
    call = { irNotEquals(arg1, arg2) },
    name = "irNotEquals",
    IrExpression::class to arg1,
    IrExpression::class to arg2,
)

/**
 * Alias for [IrBuilderWithScope.irIs] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irIsCompat(
    argument: IrExpression,
    type: IrType,
) = irExpressionHelperCompat(
    call = { irIs(argument, type) },
    name = "irIs",
    IrExpression::class to argument,
    IrType::class to type,
)

/**
 * Alias for [IrBuilderWithScope.irNotIs] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irNotIsCompat(
    argument: IrExpression,
    type: IrType,
) = irExpressionHelperCompat(
    call = { irNotIs(argument, type) },
    name = "irNotIs",
    IrExpression::class to argument,
    IrType::class to type,
)

/**
 * Alias for [IrBuilderWithScope.irFalse] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irFalseCompat() = irExpressionHelperCompat(
    call = { irFalse() },
    name = "irFalse",
)

/**
 * Alias for [IrBuilderWithScope.irImplicitCast] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irImplicitCastCompat(
    argument: IrExpression,
    type: IrType,
) = irExpressionHelperCompat(
    call = { irImplicitCast(argument, type) },
    name = "irImplicitCast",
    IrExpression::class to argument,
    IrType::class to type,
)

/**
 * Alias for [IrBuilderWithScope.irIfNull] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irIfNullCompat(
    type: IrType,
    subject: IrExpression,
    thenPart: IrExpression,
    elsePart: IrExpression,
) = irExpressionHelperCompat(
    call = { irIfNull(type, subject, thenPart, elsePart) },
    name = "irIfNull",
    IrType::class to type,
    IrExpression::class to subject,
    IrExpression::class to thenPart,
    IrExpression::class to elsePart,
)

/**
 * Alias for [IrBuilderWithScope.irIfThenElse] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irIfThenElseCompat(
    type: IrType,
    condition: IrExpression,
    thenPart: IrExpression,
    elsePart: IrExpression,
    origin: IrStatementOrigin? = null,
) = irExpressionHelperCompat(
    call = { irIfThenElse(type, condition, thenPart, elsePart, origin) },
    name = "irIfThenElse",
    IrType::class to type,
    IrExpression::class to condition,
    IrExpression::class to thenPart,
    IrExpression::class to elsePart,
    IrStatementOrigin::class to origin,
)

/**
 * Alias for [IrBuilderWithScope.irBranch] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irBranchCompat(
    condition: IrExpression,
    result: IrExpression,
) = irExpressionHelperCompat(
    call = { irBranch(condition, result) },
    name = "irBranch",
    IrExpression::class to condition,
    IrExpression::class to result,
)

/**
 * Alias for [IrBuilderWithScope.irElseBranch] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irElseBranchCompat(
    expression: IrExpression,
) = irExpressionHelperCompat(
    call = { irElseBranch(expression) },
    name = "irElseBranch",
    IrExpression::class to expression,
)

/**
 * Alias for [IrBuilderWithScope.irWhen] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irWhenCompat(
    type: IrType,
    branches: List<IrBranch>,
) = irExpressionHelperCompat(
    call = { irWhen(type, branches) },
    name = "irWhen",
    IrType::class to type,
    List::class to branches,
)

/**
 * Alias for [IrBuilderWithScope.irGetField] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irGetFieldCompat(
    receiver: IrExpression?,
    field: IrField,
    type: IrType = field.type,
) = irExpressionHelperCompat(
    call = { irGetField(receiver, field, type) },
    name = "irGetField",
    IrExpression::class to receiver,
    IrField::class to field,
    IrType::class to type,
)

/**
 * Alias for [IrBuilderWithScope.irGet] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irGetCompat(
    type: IrType,
    variable: IrValueSymbol,
) = irExpressionHelperCompat(
    call = { irGet(type, variable) },
    name = "irGet",
    IrType::class to type,
    IrValueSymbol::class to variable,
)

/**
 * Alias for [IrBuilderWithScope.irSet] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irSetCompat(
    variable: IrValueSymbol,
    value: IrExpression,
    origin: IrStatementOrigin = IrStatementOrigin.EQ,
) = irExpressionHelperCompat(
    call = { irSet(variable, value, origin) },
    name = "irSet",
    IrValueSymbol::class to variable,
    IrExpression::class to value,
    IrStatementOrigin::class to origin,
)

/**
 * Alias for [IrBuilderWithScope.irGet] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irGetCompat(
    variable: IrValueDeclaration,
) = irExpressionHelperCompat(
    call = { irGet(variable) },
    name = "irGet",
    IrValueDeclaration::class to variable,
)

/**
 * Alias for [IrBuilderWithScope.irInt] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irIntCompat(
    value: Int,
    type: IrType = context.irBuiltIns.intType,
) = irExpressionHelperCompat(
    call = { irInt(value, type) },
    name = "irInt",
    Int::class to value,
    IrType::class to type,
)

/**
 * Alias for [IrBuilderWithScope.irCallOp] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irCallOpCompat(
    callee: IrSimpleFunctionSymbol,
    type: IrType,
    dispatchReceiver: IrExpression,
    argument: IrExpression? = null,
    origin: IrStatementOrigin? = null,
) = irExpressionHelperCompat(
    call = { irCallOp(callee, type, dispatchReceiver, argument, origin) },
    name = "irCallOp",
    IrSimpleFunctionSymbol::class to callee,
    IrType::class to type,
    IrExpression::class to dispatchReceiver,
    IrExpression::class to argument,
    IrStatementOrigin::class to origin,
)

/**
 * Alias for [IrBuilderWithScope.irConcat] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irConcatCompat() = irExpressionHelperCompat(
    call = { irConcat() },
    name = "irConcat",
)

/**
 * Alias for [IrBuilderWithScope.irString] with forward compatibility for 2.2.x.
 *
 * Reverse logic when compiling with 2.2.x. Remove when support for 2.1.x is dropped.
 */
internal fun IrBuilderWithScope.irStringCompat(
    value: String,
) = irExpressionHelperCompat(
    call = { irString(value) },
    name = "irString",
    String::class to value,
)

/**
 * Helper for all the ExpressionHelpers.kt functions which had their extension receiver changed from
 * [IrBuilderWithScope] to [IrBuilder].
 *
 * Reverse logic when compiling with 2.2.x.
 */
private fun <T> IrBuilderWithScope.irExpressionHelperCompat(
    call: () -> T,
    name: String,
    vararg args: Pair<KClass<*>, Any?>,
): T = try {
    call()
} catch (noSuchMethodError: NoSuchMethodError) {
    // In 2.2.x, the extension receiver becomes `IrBuilder` instead of `IrBuilderWithScope`.
    @Suppress("UNCHECKED_CAST")
    javaClass.classLoader.loadClass("org.jetbrains.kotlin.ir.builders.ExpressionHelpersKt")
        .methods
        .findIrBuilderExtension(
            name = name,
            argTypes = args.map { it.first.java },
        )
        ?.let {
            it.invoke(
                null, // static invocation
                this, // extension receiver
                *args.map { it.second }.toTypedArray(),
            ) as T
        } ?: throw NoSuchElementException("Array contains no element matching the predicate.")
}

/**
 * Returns a [Method] defined in ExpressionHelpers.kt as an extension on [IrBuilder] if such a
 * method exists, else returns null.
 */
private fun Array<Method>.findIrBuilderExtension(
    name: String,
    argTypes: List<Class<*>>,
): Method? {
    val filtered = filter {
        it.name == name &&
            it.parameters.map { it.type } == listOf(IrBuilder::class.java) + argTypes
    }
    return when (filtered.size) {
        0 -> null
        1 -> filtered.single()
        else -> throw IllegalArgumentException("Array contains more than one matching element.")
    }
}
//endregion

/**
 * Alias for [irCall] for backward compatibility with 2.1.0 and 2.1.1x, and forward compatibility
 * with 2.2.x.
 *
 * Reverse when compiling with 2.2.x. Simplify when support for 2.1.0 & 2.1.1x is dropped. Remove
 * when support for 2.1.2x is dropped.
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
        // 2.1.20:
        irCall(
            callee = callee,
            type = type,
            typeArgumentsCount = typeArgumentsCount,
            origin = origin,
        )
    } catch (noSuchMethodError: NoSuchMethodError) {
        val functionName = "irCall"
        val expressionHelpers =
            javaClass.classLoader.loadClass("org.jetbrains.kotlin.ir.builders.ExpressionHelpersKt")
                .methods

        val kotlinTwoTwoFunction = expressionHelpers.findIrBuilderExtension(
            name = functionName,
            argTypes = listOf(
                IrSimpleFunctionSymbol::class.java,
                IrType::class.java,
                Int::class.java,
                IrStatementOrigin::class.java,
            ),
        )
        return if (kotlinTwoTwoFunction == null) {
            // 2.1.0 && 2.1.1x:
            // https://github.com/JetBrains/kotlin/blob/v2.1.0/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/builders/ExpressionHelpers.kt#L240
            expressionHelpers
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
                )
        } else {
            // 2.2.x:
            kotlinTwoTwoFunction.invoke(
                null, // static invocation
                this, // extension receiver
                callee, // callee
                type, // type
                typeArgumentsCount, // typeArgumentsCount
                origin, // origin
            )
        } as IrCall

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

/**
 * Alias for [IrElement.acceptChildrenVoid] for compatibility with 2.1.0 – 2.1.1x.
 *
 * Remove when support for 2.1.0 & 2.1.1x is dropped.
 */
internal fun IrElement.acceptChildrenVoidCompat(visitor: IrVisitorVoid) {
    try {
        acceptChildrenVoid(visitor)
    } catch (noSuchMethodError: NoSuchMethodError) {
        acceptChildren(visitor, null)
    }
}

/**
 * Alias for [IrFunction.parameters] for compatibility with 2.1.0 – 2.1.1x. Throws if the function
 * has context parameters on 2.1.1x or lower.
 *
 * Remove when support for 2.1.1x is dropped.
 */
internal val IrFunction.parametersCompat: List<IrValueParameter>
    get() = try {
        parameters
    } catch (noSuchMethodError: NoSuchMethodError) {
        require(contextReceiverParametersCount == 0) {
            "parametersCompat is not supported on functions with context parameters"
        }
        buildList {
            dispatchReceiverParameter?.let { add(it) }
            extensionReceiverParameter?.let { add(it) }
            valueParameters.forEach {
                add(it)
            }
        }
    }
