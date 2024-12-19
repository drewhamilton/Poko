package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irIfThenReturnFalse
import org.jetbrains.kotlin.ir.builders.irIfThenReturnTrue
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNotIs
import org.jetbrains.kotlin.ir.builders.irReturnTrue
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isArrayOrPrimitiveArray
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Generate the body of the equals method. Adapted from
 * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateEqualsMethodBody].
 */
internal fun IrBlockBodyBuilder.generateEqualsMethodBody(
    pokoAnnotation: ClassId,
    context: IrPluginContext,
    irClass: IrClass,
    functionDeclaration: IrFunction,
    classProperties: List<IrProperty>,
    messageCollector: MessageCollector,
) {
    val irType = irClass.defaultType
    fun irOther(): IrExpression = IrGetValueImpl(functionDeclaration.valueParameters.single())

    +irIfThenReturnTrue(irEqeqeq(receiver(functionDeclaration), irOther()))
    +irIfThenReturnFalse(irNotIs(irOther(), irType))

    val otherWithCast = irTemporary(irImplicitCast(irOther(), irType), "other_with_cast")
    for (property in classProperties) {
        val field = property.backingField!!
        val arg1 = irGetField(receiver(functionDeclaration), field)
        val arg2 = irGetField(irGet(irType, otherWithCast.symbol), field)
        val irNotEquals = when {
            property.hasReadArrayContentAnnotation(pokoAnnotation) -> {
                irNot(
                    irArrayContentDeepEquals(
                        context = context,
                        receiver = arg1,
                        argument = arg2,
                        property = property,
                        messageCollector = messageCollector,
                    ),
                )
            }

            else -> {
                irNotEquals(arg1, arg2)
            }
        }
        +irIfThenReturnFalse(irNotEquals)
    }
    +irReturnTrue()
}

/**
 * Generates IR code that checks the equality of [receiver] and [argument] by content. If [property]
 * type is not an array type, but may be an array at runtime, generates a runtime type check.
 */
private fun IrBuilderWithScope.irArrayContentDeepEquals(
    context: IrPluginContext,
    receiver: IrExpression,
    argument: IrExpression,
    property: IrProperty,
    messageCollector: MessageCollector,
): IrExpression {
    val propertyType = property.type
    val propertyClassifier = propertyType.classifierOrFail

    val isArray = propertyClassifier.isArrayOrPrimitiveArray(context.irBuiltIns)
    if (!isArray) {
        val mayBeRuntimeArray = propertyClassifier.mayBeRuntimeArray(context)
        return if (mayBeRuntimeArray) {
            irRuntimeArrayContentDeepEquals(context, receiver, argument)
        } else {
            messageCollector.reportErrorOnProperty(
                property = property,
                message = "@ArrayContentBased on property of type <${propertyType.render()}> not supported",
            )
            irEquals(receiver, argument)
        }
    }

    return irCallContentDeepEquals(
        context = context,
        classifier = propertyClassifier,
        receiver = receiver,
        argument = argument,
    )
}

/**
 * Generates IR code that checks the type of [receiver] at runtime, and performs an array content
 * equality check against [argument] if the type is an array type.
 */
private fun IrBuilderWithScope.irRuntimeArrayContentDeepEquals(
    context: IrPluginContext,
    receiver: IrExpression,
    argument: IrExpression,
): IrExpression {
    return irWhen(
        type = context.irBuiltIns.booleanType,
        branches = listOf(
            irArrayTypeCheckAndContentDeepEqualsBranch(
                context = context,
                receiver = receiver,
                argument = argument,
                classSymbol = context.irBuiltIns.arrayClass,
            ),

            // Map each primitive type to a `when` branch covering its respective primitive array
            // type:
            *PrimitiveType.entries.map { primitiveType ->
                irArrayTypeCheckAndContentDeepEqualsBranch(
                    context = context,
                    receiver = receiver,
                    argument = argument,
                    classSymbol = primitiveType.toPrimitiveArrayClassSymbol(context),
                )
            }.toTypedArray(),

            irElseBranch(
                irEquals(receiver, argument),
            ),
        ),
    )
}

/**
 * Generates a runtime `when` branch checking for content deep equality of [receiver] and
 * [argument]. The branch is only executed if [receiver] is an instance of [classSymbol].
 */
private fun IrBuilderWithScope.irArrayTypeCheckAndContentDeepEqualsBranch(
    context: IrPluginContext,
    receiver: IrExpression,
    argument: IrExpression,
    classSymbol: IrClassSymbol,
): IrBranch {
    val type = classSymbol.createArrayType(context)
    return irBranch(
        condition = irIs(receiver, type),
        result = irIfThenElse(
            type = context.irBuiltIns.booleanType,
            condition = irIs(argument, type),
            thenPart = irCallContentDeepEquals(
                context = context,
                classifier = classSymbol,
                receiver = irImplicitCast(receiver, type),
                argument = irImplicitCast(argument, type),
            ),
            elsePart = irFalse(),
        ),
    )
}

private fun IrBuilderWithScope.irCallContentDeepEquals(
    context: IrPluginContext,
    classifier: IrClassifierSymbol,
    receiver: IrExpression,
    argument: IrExpression,
): IrExpression {
    return irCallCompat(
        callee = findContentDeepEqualsFunctionSymbol(context, classifier),
        type = context.irBuiltIns.booleanType,
        valueArgumentsCount = 1,
        typeArgumentsCount = 1,
    ).apply {
        extensionReceiver = receiver
        putValueArgument(0, argument)
    }
}

/**
 * Finds `contentDeepEquals` function if [classifier] represents a typed array, or `contentEquals`
 * function if it represents a primitive array.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun findContentDeepEqualsFunctionSymbol(
    context: IrPluginContext,
    classifier: IrClassifierSymbol,
): IrSimpleFunctionSymbol {
    val callableName = if (classifier == context.irBuiltIns.arrayClass) {
        "contentDeepEquals"
    } else {
        "contentEquals"
    }
    return context.referenceFunctions(
        callableId = CallableId(
            packageName = FqName("kotlin.collections"),
            callableName = Name.identifier(callableName),
        ),
    ).single { functionSymbol ->
        // Find the single function with the relevant array type and disambiguate against the
        // older non-nullable receiver overload:
        functionSymbol.owner.extensionReceiverParameter?.type?.let {
            it.classifierOrNull == classifier && it.isNullable()
        } ?: false
    }
}
