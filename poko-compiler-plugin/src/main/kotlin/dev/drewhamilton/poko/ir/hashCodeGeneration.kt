package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isUInt
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isArrayOrPrimitiveArray
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Generate the body of the hashCode method. Adapted from
 * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateHashCodeMethodBody].
 */
internal fun IrBlockBodyBuilder.generateHashCodeMethodBody(
    pokoAnnotation: ClassId,
    context: IrPluginContext,
    functionDeclaration: IrFunction,
    classProperties: List<IrProperty>,
    messageCollector: MessageCollector,
) {
    if (classProperties.isEmpty()) {
        +irReturn(irInt(0))
        return
    } else if (classProperties.size == 1) {
        +irReturn(
            getHashCodeOfProperty(
                pokoAnnotation = pokoAnnotation,
                context = context,
                function = functionDeclaration,
                property = classProperties[0],
                messageCollector = messageCollector,
            ),
        )
        return
    }

    val irIntType = context.irBuiltIns.intType

    val irResultVar = IrVariableImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = IrDeclarationOrigin.DEFINED,
        symbol = IrVariableSymbolImpl(),
        name = Name.identifier("result"),
        type = irIntType,
        isVar = true,
        isConst = false,
        isLateinit = false,
    ).also {
        it.parent = functionDeclaration
        it.initializer = getHashCodeOfProperty(
            pokoAnnotation = pokoAnnotation,
            context = context,
            function = functionDeclaration,
            property = classProperties[0],
            messageCollector = messageCollector,
        )
    }
    +irResultVar

    for (property in classProperties.drop(1)) {
        val shiftedResult = irCallOp(
            callee = context.irBuiltIns.intTimesSymbol,
            type = irIntType,
            dispatchReceiver = irGet(irResultVar),
            argument = irInt(31),
        )
        val rhs = irCallOp(
            callee = context.irBuiltIns.intPlusSymbol,
            type = irIntType,
            dispatchReceiver = shiftedResult,
            argument = getHashCodeOfProperty(
                pokoAnnotation = pokoAnnotation,
                context = context,
                function = functionDeclaration,
                property = property,
                messageCollector = messageCollector,
            ),
        )
        +irSet(irResultVar.symbol, rhs)
    }

    +irReturn(irGet(irResultVar))
}

/**
 * Generates the hashcode-computing code for [property].
 */
private fun IrBlockBodyBuilder.getHashCodeOfProperty(
    pokoAnnotation: ClassId,
    context: IrPluginContext,
    function: IrFunction,
    property: IrProperty,
    messageCollector: MessageCollector,
): IrExpression {
    val field = property.backingField!!
    val irGetField = { irGetField(receiver(function), field) }
    return when {
        property.type.isNullable() -> irIfNull(
            type = context.irBuiltIns.intType,
            subject = irGetField(),
            thenPart = irInt(0),
            elsePart = getHashCodeOf(pokoAnnotation, context, property, irGetField(), messageCollector)
        )
        else -> getHashCodeOf(pokoAnnotation, context, property, irGetField(), messageCollector)
    }
}

/**
 * Symbol-retrieval adapted from
 * [org.jetbrains.kotlin.fir.backend.generators.DataClassMembersGenerator].
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrBlockBodyBuilder.getHashCodeOf(
    pokoAnnotation: ClassId,
    context: IrPluginContext,
    property: IrProperty,
    value: IrExpression,
    messageCollector: MessageCollector,
): IrExpression {
    // Fast path for integers which are already their own hashCode value.
    if (property.type.isInt()) {
        return value
    }
    if (property.type.isUInt()) {
        val uintToInt = context.referenceClass(StandardNames.FqNames.uInt)!!
            .functions
            .single { it.owner.name.asString() == "toInt" }
        return irCall(uintToInt).apply {
            dispatchReceiver = value
        }
    }

    val hasArrayContentBasedAnnotation = property.hasReadArrayContentAnnotation(pokoAnnotation)
    val classifier = property.type.classifierOrNull

    if (hasArrayContentBasedAnnotation && classifier.mayBeRuntimeArray(context)) {
        return irRuntimeArrayContentDeepHashCode(context, value)
    }

    // Non-null if content-based hashCode will be used, else null:
    val contentHashCodeFunctionSymbol = if (hasArrayContentBasedAnnotation) {
        maybeFindArrayContentHashCodeFunction(context, property, messageCollector)
    } else {
        null
    }
    val hashCodeFunctionSymbol = contentHashCodeFunctionSymbol
        ?: getStandardHashCodeFunctionSymbol(classifier)

    return irCallHashCodeFunction(hashCodeFunctionSymbol, value)
}

/**
 * Generates a `when` branch that checks the runtime type of the [value] instance and invokes
 * `contentDeepHashCode` or `contentHashCode` for typed arrays and primitive arrays, respectively.
 */
private fun IrBlockBodyBuilder.irRuntimeArrayContentDeepHashCode(
    context: IrPluginContext,
    value: IrExpression,
): IrExpression {
    return irWhen(
        type = context.irBuiltIns.intType,
        branches = listOf(
            irArrayTypeCheckAndContentDeepHashCodeBranch(
                context = context,
                value = value,
                classSymbol = context.irBuiltIns.arrayClass,
            ),

            // Map each primitive type to a `when` branch covering its respective primitive array
            // type:
            *PrimitiveType.entries.map { primitiveType ->
                irArrayTypeCheckAndContentDeepHashCodeBranch(
                    context = context,
                    value = value,
                    classSymbol = primitiveType.toPrimitiveArrayClassSymbol(context),
                )
            }.toTypedArray(),

            irElseBranch(
                irIfNull(
                    type = context.irBuiltIns.intType,
                    subject = value,
                    thenPart = irInt(0),
                    elsePart = irCallHashCodeFunction(
                        hashCodeFunctionSymbol = getStandardHashCodeFunctionSymbol(
                            classifier = value.type.classifierOrNull,
                        ),
                        value = value,
                    ),
                ),
            ),
        ),
    )
}

/**
 * Generates a runtime `when` branch computing the content deep hashCode of [value]. The branch is
 * only executed if [value] is an instance of [classSymbol].
 */
private fun IrBlockBodyBuilder.irArrayTypeCheckAndContentDeepHashCodeBranch(
    context: IrPluginContext,
    value: IrExpression,
    classSymbol: IrClassSymbol,
): IrBranch {
    val type = classSymbol.createArrayType(context)
    return irBranch(
        condition = irIs(value, type),
        result = irCall(
            callee = findArrayContentDeepHashCodeFunction(context, classSymbol),
            type = context.irBuiltIns.intType,
        ).apply {
            @OptIn(DeprecatedForRemovalCompilerApi::class) // FIXME
            extensionReceiver = irImplicitCast(value, type)
        }
    )
}

/**
 * Returns contentDeepHashCode function symbol if it is an appropriate option for [property],
 * else returns null. [property] must have an array type.
 */
private fun maybeFindArrayContentHashCodeFunction(
    context: IrPluginContext,
    property: IrProperty,
    messageCollector: MessageCollector,
): IrSimpleFunctionSymbol? {
    val propertyClassifier = property.type.classifierOrFail

    val isArray = propertyClassifier.isArrayOrPrimitiveArray(context.irBuiltIns)
    if (!isArray) {
        messageCollector.reportErrorOnProperty(
            property = property,
            message = "@ArrayContentBased on property of type <${property.type.render()}> not supported",
        )
        return null
    }

    return findArrayContentDeepHashCodeFunction(context, propertyClassifier)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun findArrayContentDeepHashCodeFunction(
    context: IrPluginContext,
    propertyClassifier: IrClassifierSymbol,
): IrSimpleFunctionSymbol {
    val callableName = if (propertyClassifier == context.irBuiltIns.arrayClass) {
        "contentDeepHashCode"
    } else {
        "contentHashCode"
    }
    return context.referenceFunctions(
        callableId = CallableId(
            packageName = FqName("kotlin.collections"),
            callableName = Name.identifier(callableName),
        ),
    ).single { functionSymbol ->
        // Disambiguate against the older non-nullable receiver overload:
        @OptIn(DeprecatedForRemovalCompilerApi::class) // FIXME
        functionSymbol.owner.extensionReceiverParameter?.type?.let {
            it.classifierOrNull == propertyClassifier && it.isNullable()
        } ?: false
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrBlockBodyBuilder.getStandardHashCodeFunctionSymbol(
    classifier: IrClassifierSymbol?,
): IrSimpleFunctionSymbol = when {
    classifier.isArrayOrPrimitiveArray(context.irBuiltIns) ->
        context.irBuiltIns.dataClassArrayMemberHashCodeSymbol
    classifier is IrClassSymbol ->
        getHashCodeFunctionForClass(classifier.owner)
    classifier is IrTypeParameterSymbol ->
        getHashCodeFunctionForClass(classifier.owner.erasedUpperBound)
    else ->
        error("Unknown classifier kind $classifier")
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrBlockBodyBuilder.getHashCodeFunctionForClass(
    irClass: IrClass
): IrSimpleFunctionSymbol {
    val explicitHashCodeDeclaration = irClass.functions.singleOrNull { function ->
        function.name.asString() == "hashCode" &&
            function.parameters == function.parameters.filter {
                it.kind == IrParameterKind.DispatchReceiver
            }
    }
    return explicitHashCodeDeclaration?.symbol
        ?: context.irBuiltIns.anyClass.functions.single { it.owner.name.asString() == "hashCode" }
}

@OptIn(UnsafeDuringIrConstructionAPI::class, DeprecatedForRemovalCompilerApi::class) // FIXME
private fun IrBlockBodyBuilder.irCallHashCodeFunction(
    hashCodeFunctionSymbol: IrSimpleFunctionSymbol,
    value: IrExpression,
): IrExpression {
    check(hashCodeFunctionSymbol.isBound) { "$hashCodeFunctionSymbol is not bound" }

    // Poko modification: check for extension receiver for contentDeepHashCode case
    val (hasDispatchReceiver, hasExtensionReceiver) = with(hashCodeFunctionSymbol.owner) {
        (dispatchReceiverParameter != null) to (extensionReceiverParameter != null)
    }
    return irCall(
        callee = hashCodeFunctionSymbol,
        type = context.irBuiltIns.intType,
        typeArgumentsCount = 0,
    ).apply {
        when {
            hasDispatchReceiver -> dispatchReceiver = value
            hasExtensionReceiver -> extensionReceiver = value
            else -> putValueArgument(0, value)
        }
    }
}
