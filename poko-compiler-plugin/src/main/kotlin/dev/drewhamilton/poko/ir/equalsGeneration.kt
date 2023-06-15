package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextInterface
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfThenReturnFalse
import org.jetbrains.kotlin.ir.builders.irIfThenReturnTrue
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNotIs
import org.jetbrains.kotlin.ir.builders.irReturnTrue
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * True if the function's signature matches the standard `equals` function signature.
 */
context(IrGeneratorContextInterface)
internal fun IrFunction.isEquals(): Boolean {
    val valueParameters = valueParameters
    return name == Name.identifier("equals") &&
        returnType == irBuiltIns.booleanType &&
        valueParameters.size == 1 && valueParameters[0].type == irBuiltIns.anyNType
}

/**
 * Generate the body of the equals method. Adapted from
 * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateEqualsMethodBody].
 */
context(IrPluginContext)
internal fun IrBlockBodyBuilder.generateEqualsMethodBody(
    irClass: IrClass,
    functionDeclaration: IrFunction,
    classProperties: List<IrProperty>,
    messageCollector: MessageCollector,
) {
    val irType = irClass.defaultType
    fun irOther(): IrExpression = IrGetValueImpl(functionDeclaration.valueParameters.single())

    if (!irClass.isSingleFieldValueClass) {
        +irIfThenReturnTrue(irEqeqeq(functionDeclaration.receiver(), irOther()))
    }

    +irIfThenReturnFalse(irNotIs(irOther(), irType))

    val otherWithCast = irTemporary(irAs(irOther(), irType), "other_with_cast")
    for (property in classProperties) {
        val field = property.backingField!!
        val arg1 = irGetField(functionDeclaration.receiver(), field)
        val arg2 = irGetField(irGet(irType, otherWithCast.symbol), field)
        val irNotEquals = when {
            property.hasArrayContentBasedAnnotation() -> {
                irNot(
                    irArrayContentDeepEquals(
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
context(IrPluginContext)
private fun IrBuilderWithScope.irArrayContentDeepEquals(
    receiver: IrExpression,
    argument: IrExpression,
    property: IrProperty,
    messageCollector: MessageCollector,
): IrExpression {
    val propertyType = property.type
    val propertyClassifier = propertyType.classifierOrFail

    if (!propertyClassifier.isArrayOrPrimitiveArray(context)) {
        return if (propertyClassifier.mayBeRuntimeArray(context)) {
            irRuntimeArrayContentDeepEquals(receiver, argument)
        } else {
            messageCollector.reportErrorOnProperty(
                property = property,
                message = "@ArrayContentBased on property of type <${propertyType.render()}> not supported",
            )
            irEquals(receiver, argument)
        }
    }

    return irCallContentDeepEquals(
        classifier = propertyClassifier,
        receiver = receiver,
        argument = argument,
    )
}

/**
 * Generates IR code that checks the type of [receiver] at runtime, and performs an array content
 * equality check against [argument] if the type is an array type.
 */
context(IrPluginContext)
private fun IrBuilderWithScope.irRuntimeArrayContentDeepEquals(
    receiver: IrExpression,
    argument: IrExpression,
): IrExpression {
    return irWhen(
        type = context.irBuiltIns.booleanType,
        branches = listOf(
            irArrayTypeCheckAndContentDeepEqualsBranch(
                receiver = receiver,
                argument = argument,
                classSymbol = context.irBuiltIns.arrayClass,
            ),

            irArrayTypeCheckAndContentDeepEqualsBranch(
                receiver = receiver,
                argument = argument,
                classSymbol = with(context) { PrimitiveType.BOOLEAN.toPrimitiveArrayClassSymbol() },
            ),

            // TODO: Primitive arrays

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
context(IrPluginContext)
private fun IrBuilderWithScope.irArrayTypeCheckAndContentDeepEqualsBranch(
    receiver: IrExpression,
    argument: IrExpression,
    classSymbol: IrClassSymbol,
): IrBranch {
    val type = with(context) { classSymbol.createArrayType() }
    return irBranch(
        condition = irIs(receiver, type),
        result = irCall(
            callee = context.irBuiltIns.andandSymbol,
            type = context.irBuiltIns.booleanType,
            valueArgumentsCount = 2,
        ).apply {
            putValueArgument(
                index = 0,
                valueArgument = irIs(argument, type),
            )
            putValueArgument(
                index = 1,
                valueArgument = irCallContentDeepEquals(
                    classifier = classSymbol,
                    receiver = receiver,
                    argument = argument,
                ),
            )
        },
    )
}

context(IrPluginContext)
private fun IrBuilderWithScope.irCallContentDeepEquals(
    classifier: IrClassifierSymbol,
    receiver: IrExpression,
    argument: IrExpression,
): IrExpression {
    return irCall(
        callee = findContentDeepEqualsFunctionSymbol(classifier),
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
context(IrPluginContext)
private fun IrBuilderWithScope.findContentDeepEqualsFunctionSymbol(
    classifier: IrClassifierSymbol,
): IrSimpleFunctionSymbol {
    val callableName = if (classifier == context.irBuiltIns.arrayClass) {
        "contentDeepEquals"
    } else {
        "contentEquals"
    }
    return referenceFunctions(
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
