package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextInterface
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
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

context(IrGeneratorContextInterface)
internal fun IrFunction.isToString(): Boolean {
    return name == Name.identifier("toString") &&
        returnType == irBuiltIns.stringType &&
        valueParameters.isEmpty()
}

/**
 * Generate the body of the toString method. Adapted from
 * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateToStringMethodBody].
 */
context(IrPluginContext)
internal fun IrBlockBodyBuilder.generateToStringMethodBody(
    irClass: IrClass,
    functionDeclaration: IrFunction,
    classProperties: List<IrProperty>,
    messageCollector: MessageCollector,
) {
    val irConcat = irConcat()
    irConcat.addArgument(irString(irClass.name.asString() + "("))

    var first = true
    for (property in classProperties) {
        if (!first) irConcat.addArgument(irString(", "))

        irConcat.addArgument(irString(property.name.asString() + "="))

        val propertyValue = irGetField(functionDeclaration.receiver(), property.backingField!!)

        val classifier = property.type.classifierOrNull
        val hasArrayContentBasedAnnotation = property.hasArrayContentBasedAnnotation()
        val propertyStringValue = when {
            hasArrayContentBasedAnnotation && with(context) { classifier.mayBeRuntimeArray() } -> {
                val field = property.backingField!!
                val instance = irGetField(functionDeclaration.receiver(), field)
                irRuntimeArrayContentDeepToString(instance)
            }

            hasArrayContentBasedAnnotation -> {
                val toStringFunctionSymbol = maybeFindArrayDeepToStringFunction(
                    property = property,
                    messageCollector = messageCollector
                ) ?: context.irBuiltIns.dataClassArrayMemberToStringSymbol
                irCallToStringFunction(
                    toStringFunctionSymbol = toStringFunctionSymbol,
                    value = propertyValue,
                )
            }

            with(context) { classifier.isArrayOrPrimitiveArray() } -> {
                irCallToStringFunction(
                    toStringFunctionSymbol = context.irBuiltIns.dataClassArrayMemberToStringSymbol,
                    value = propertyValue,
                )
            }

            else -> propertyValue
        }

        irConcat.addArgument(propertyStringValue)
        first = false
    }
    irConcat.addArgument(irString(")"))
    +irReturn(irConcat)
}

/**
 * Returns `contentDeepToString` function symbol if it is an appropriate option for [property],
 * else returns null.
 */
context(IrPluginContext)
private fun IrBlockBodyBuilder.maybeFindArrayDeepToStringFunction(
    property: IrProperty,
    messageCollector: MessageCollector,
): IrSimpleFunctionSymbol? {
    val propertyClassifier = property.type.classifierOrFail

    val isArray = with(context) { propertyClassifier.isArrayOrPrimitiveArray() }
    if (!isArray) {
        messageCollector.reportErrorOnProperty(
            property = property,
            message = "@ArrayContentBased on property of type <${property.type.render()}> not supported",
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
 * Generates a `when` branch that checks the runtime type of the [value] instance and invokes
 * `contentDeepToString` or `contentToString` for typed arrays and primitive arrays, respectively.
 */
context(IrPluginContext)
private fun IrBlockBodyBuilder.irRuntimeArrayContentDeepToString(
    value: IrExpression,
): IrExpression {
    return irWhen(
        type = context.irBuiltIns.stringType,
        branches = listOf(
            irArrayTypeCheckAndContentDeepToStringBranch(
                value = value,
                classSymbol = context.irBuiltIns.arrayClass,
            ),

            // Map each primitive type to a `when` branch covering its respective primitive array
            // type:
            *PrimitiveType.values().map { primitiveType ->
                irArrayTypeCheckAndContentDeepToStringBranch(
                    value = value,
                    classSymbol = with(context) { primitiveType.toPrimitiveArrayClassSymbol() },
                )
            }.toTypedArray(),

            irElseBranch(
                irCallToStringFunction(
                    toStringFunctionSymbol = context.irBuiltIns.extensionToString,
                    value = value,
                ),
            ),
        ),
    )
}

/**
 * Generates a runtime `when` branch computing the content deep toString of [value]. The branch is
 * only executed if [value] is an instance of [classSymbol].
 */
context(IrPluginContext)
private fun IrBlockBodyBuilder.irArrayTypeCheckAndContentDeepToStringBranch(
    value: IrExpression,
    classSymbol: IrClassSymbol,
): IrBranch {
    val type = classSymbol.createArrayType()
    return irBranch(
        condition = irIs(value, type),
        result = irCallToStringFunction(
            toStringFunctionSymbol = findContentDeepToStringFunctionSymbol(classSymbol),
            value = value,
        ),
    )
}

/**
 * Finds `contentDeepToString` function if [propertyClassifier] is a typed array, or
 * `contentToString` function if it is a primitive array.
 */
context(IrPluginContext)
private fun IrBuilderWithScope.findContentDeepToStringFunctionSymbol(
    propertyClassifier: IrClassifierSymbol,
): IrSimpleFunctionSymbol {
    val callableName = if (propertyClassifier == context.irBuiltIns.arrayClass) {
        "contentDeepToString"
    } else {
        "contentToString"
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
            it.classifierOrNull == propertyClassifier && it.isNullable()
        } ?: false
    }
}

private fun IrBlockBodyBuilder.irCallToStringFunction(
    toStringFunctionSymbol: IrSimpleFunctionSymbol,
    value: IrExpression,
): IrExpression {
    return irCall(
        callee = toStringFunctionSymbol,
        type = context.irBuiltIns.stringType,
    ).apply {
        // Poko modification: check for extension receiver for contentDeepToString
        val hasExtensionReceiver =
            toStringFunctionSymbol.owner.extensionReceiverParameter != null
        if (hasExtensionReceiver) {
            extensionReceiver = value
        } else {
            putValueArgument(0, value)
        }
    }
}
