package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.isArrayOrPrimitiveArray
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Generate the body of the toString method. Adapted from
 * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateToStringMethodBody].
 */
@UnsafeDuringIrConstructionAPI
internal fun IrBlockBodyBuilder.generateToStringMethodBody(
    pokoAnnotation: ClassId,
    context: IrPluginContext,
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

        val propertyValue = irGetField(receiver(functionDeclaration), property.backingField!!)

        val classifier = property.type.classifierOrNull
        val hasReadArrayContentAnnotation = property.hasReadArrayContentAnnotation(pokoAnnotation)
        val propertyStringValue = when {
            hasReadArrayContentAnnotation && classifier.mayBeRuntimeArray(context) -> {
                val field = property.backingField!!
                val instance = irGetField(receiver(functionDeclaration), field)
                irRuntimeArrayContentDeepToString(context, instance)
            }

            hasReadArrayContentAnnotation -> {
                val toStringFunctionSymbol = maybeFindArrayDeepToStringFunction(
                    context = context,
                    property = property,
                    messageCollector = messageCollector
                ) ?: context.irBuiltIns.dataClassArrayMemberToStringSymbol
                irCallToStringFunction(
                    toStringFunctionSymbol = toStringFunctionSymbol,
                    value = propertyValue,
                )
            }

            classifier.isArrayOrPrimitiveArray(context.irBuiltIns) -> {
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
@UnsafeDuringIrConstructionAPI
private fun maybeFindArrayDeepToStringFunction(
    context: IrPluginContext,
    property: IrProperty,
    messageCollector: MessageCollector,
): IrSimpleFunctionSymbol? {
    val propertyClassifier = property.type.classifierOrFail

    val isArray = propertyClassifier.isArrayOrPrimitiveArray(context.irBuiltIns)
    if (!isArray) {
        messageCollector.reportErrorOnProperty(
            property = property,
            message = "@ReadArrayContent is only supported on properties with array type or `Any` type",
        )
        return null
    }

    return findContentDeepToStringFunctionSymbol(context, propertyClassifier)
}

/**
 * Generates a `when` branch that checks the runtime type of the [value] instance and invokes
 * `contentDeepToString` or `contentToString` for typed arrays and primitive arrays, respectively.
 */
@UnsafeDuringIrConstructionAPI
private fun IrBlockBodyBuilder.irRuntimeArrayContentDeepToString(
    context: IrPluginContext,
    value: IrExpression,
): IrExpression {
    return irWhen(
        type = context.irBuiltIns.stringType,
        branches = listOf(
            irArrayTypeCheckAndContentDeepToStringBranch(
                context = context,
                value = value,
                classSymbol = context.irBuiltIns.arrayClass,
            ),

            // Map each primitive type to a `when` branch covering its respective primitive array
            // type:
            *PrimitiveType.entries.map { primitiveType ->
                irArrayTypeCheckAndContentDeepToStringBranch(
                    context = context,
                    value = value,
                    classSymbol = primitiveType.toPrimitiveArrayClassSymbol(context),
                )
            }.toTypedArray(),

            irElseBranch(
                irCallToStringFunction(
                    toStringFunctionSymbol = context.findExtensionToStringFunctionSymbol(),
                    value = value,
                ),
            ),
        ),
    )
}

@UnsafeDuringIrConstructionAPI
private fun IrPluginContext.findExtensionToStringFunctionSymbol(): IrSimpleFunctionSymbol {
    val callableId = CallableId(
        callableName = OperatorNameConventions.TO_STRING,
        packageName = StandardClassIds.BASE_KOTLIN_PACKAGE,
    )
    return finderForBuiltins().findFunctions(callableId = callableId)
        .filter { simpleFunctionSymbol ->
            val extensionReceiverParameter = simpleFunctionSymbol.owner.parameters
                .firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
            extensionReceiverParameter?.type?.isNullableAny() == true
        }
        // TODO: Simplify to a `.single()` call when non-K2 support is dropped
        .also { simpleFunctionSymbols ->
            if (simpleFunctionSymbols.size > 1) {
                val symbolStrings = simpleFunctionSymbols
                    .map { it.toString() }
                    .toSet()
                if (symbolStrings.size > 1) {
                    val message = buildString {
                        append("Found multiple matching extensionToString functions:")
                        symbolStrings.forEach { symbolString ->
                            append("\n")
                            append(symbolString)
                        }
                    }
                    throw IllegalArgumentException(message)
                }
            }
        }
        .first()
}

/**
 * Generates a runtime `when` branch computing the content deep toString of [value]. The branch is
 * only executed if [value] is an instance of [classSymbol].
 */
@UnsafeDuringIrConstructionAPI
private fun IrBlockBodyBuilder.irArrayTypeCheckAndContentDeepToStringBranch(
    context: IrPluginContext,
    value: IrExpression,
    classSymbol: IrClassSymbol,
): IrBranch {
    val type = classSymbol.createArrayType(context)
    return irBranch(
        condition = irIs(value, type),
        result = irCallToStringFunction(
            toStringFunctionSymbol = findContentDeepToStringFunctionSymbol(context, classSymbol),
            value = irImplicitCast(value, type),
        ),
    )
}

/**
 * Finds `contentDeepToString` function if [propertyClassifier] is a typed array, or
 * `contentToString` function if it is a primitive array.
 */
@UnsafeDuringIrConstructionAPI
private fun findContentDeepToStringFunctionSymbol(
    context: IrPluginContext,
    propertyClassifier: IrClassifierSymbol,
): IrSimpleFunctionSymbol {
    val callableName = if (propertyClassifier == context.irBuiltIns.arrayClass) {
        "contentDeepToString"
    } else {
        "contentToString"
    }
    return context.finderForBuiltins().findFunctions(
        callableId = CallableId(
            packageName = FqName("kotlin.collections"),
            callableName = Name.identifier(callableName),
        ),
    ).single { functionSymbol ->
        // Find the single function with the relevant array type and disambiguate against the
        // older non-nullable receiver overload:
        val extensionReceiverParameter = functionSymbol.owner.parameters
            .singleOrNull { it.kind == IrParameterKind.ExtensionReceiver }
        return@single extensionReceiverParameter?.type?.let {
            it.classifierOrNull == propertyClassifier && it.isNullable()
        } ?: false
    }
}

@UnsafeDuringIrConstructionAPI
private fun IrBlockBodyBuilder.irCallToStringFunction(
    toStringFunctionSymbol: IrSimpleFunctionSymbol,
    value: IrExpression,
): IrExpression {
    return irCall(
        callee = toStringFunctionSymbol,
        type = context.irBuiltIns.stringType,
    ).apply {
        toStringFunctionSymbol.owner.parameters.forEach {
            arguments.set(
                parameter = it,
                value = when (it.kind) {
                    IrParameterKind.ExtensionReceiver -> value
                    IrParameterKind.Regular -> value
                    else -> throw IllegalArgumentException("toString unknown param type")
                }
            )
        }
    }
}
