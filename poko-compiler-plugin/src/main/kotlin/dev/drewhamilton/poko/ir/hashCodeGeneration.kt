package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextInterface
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * True if the function's signature matches the standard `hashCode` function signature.
 */
context(IrGeneratorContextInterface)
internal fun IrFunction.isHashCode(): Boolean =
    name == Name.identifier("hashCode") &&
        returnType == irBuiltIns.intType &&
        valueParameters.isEmpty()

/**
 * Generate the body of the hashCode method. Adapted from
 * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateHashCodeMethodBody].
 */
context(IrPluginContext)
internal fun IrBlockBodyBuilder.generateHashCodeMethodBody(
    functionDeclaration: IrFunction,
    classProperties: List<IrProperty>,
    messageCollector: MessageCollector,
) {
    if (classProperties.isEmpty()) {
        +irReturn(irInt(0))
        return
    } else if (classProperties.size == 1) {
        +irReturn(getHashCodeOfProperty(functionDeclaration, classProperties[0], messageCollector))
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
            argument = getHashCodeOfProperty(functionDeclaration, property, messageCollector),
        )
        +irSet(irResultVar.symbol, rhs)
    }

    +irReturn(irGet(irResultVar))
}

/**
 * Generates the hashcode-computing code for [property].
 */
context(IrPluginContext)
private fun IrBlockBodyBuilder.getHashCodeOfProperty(
    function: IrFunction,
    property: IrProperty,
    messageCollector: MessageCollector,
): IrExpression {
    val field = property.backingField!!
    val irGetField = { irGetField(function.receiver(), field) }
    return when {
        property.type.isNullable() -> irIfNull(
            type = context.irBuiltIns.intType,
            subject = irGetField(),
            thenPart = irInt(0),
            elsePart = getHashCodeOf(property, irGetField(), messageCollector)
        )
        else -> getHashCodeOf(property, irGetField(), messageCollector)
    }
}

/**
 * Symbol-retrieval adapted from
 * [org.jetbrains.kotlin.fir.backend.generators.DataClassMembersGenerator].
 */
context(IrPluginContext)
private fun IrBlockBodyBuilder.getHashCodeOf(
    property: IrProperty,
    value: IrExpression,
    messageCollector: MessageCollector,
): IrExpression {
    val hasArrayContentBasedAnnotation =
        property.hasAnnotation(ArrayContentBasedAnnotation.asSingleFqName())
    val classifier = property.type.classifierOrNull
    val mayBeRuntimeArray = classifier?.mayBeRuntimeArray(context) == true

    if (hasArrayContentBasedAnnotation && mayBeRuntimeArray) {
        return irRuntimeArrayContentDeepHashCode(value)
    }

    // Non-null if deep hashCode will be used, else null:
    val deepHashCodeFunctionSymbol = if (hasArrayContentBasedAnnotation) {
        maybeFindArrayDeepHashCodeFunction(property, messageCollector)
    } else {
        null
    }
    val hashCodeFunctionSymbol = deepHashCodeFunctionSymbol
        ?: getStandardHashCodeFunctionSymbol(classifier)

    return irCallHashCodeFunction(hashCodeFunctionSymbol, value)
}

/**
 * Generates a `when` branch that checks the runtime type of the [value] instance and invokes
 * `contentDeepHashCode` or `contentHashCode` for typed arrays and primitive arrays, respectively.
 */
context(IrPluginContext)
private fun IrBlockBodyBuilder.irRuntimeArrayContentDeepHashCode(
    value: IrExpression,
): IrExpression {
    val starArrayType = starArrayType()
    return irWhen(
        type = context.irBuiltIns.intType,
        branches = listOf(
            irBranch(
                condition = irIs(
                    argument = value,
                    type = starArrayType,
                ),
                result = irCall(
                    callee = findArrayDeepHashCodeFunction(context.irBuiltIns.arrayClass),
                    type = context.irBuiltIns.intType,
                ).apply {
                    extensionReceiver = value
                }
            ),

            // TODO: Primitive arrays

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
 * Returns contentDeepHashCode function symbol if it is an appropriate option for [property],
 * else returns null. [property] must have an array type.
 */
context(IrPluginContext)
private fun IrBlockBodyBuilder.maybeFindArrayDeepHashCodeFunction(
    property: IrProperty,
    messageCollector: MessageCollector,
): IrSimpleFunctionSymbol? {
    val propertyClassifier = property.type.classifierOrFail

    if (!propertyClassifier.isArrayOrPrimitiveArray(context)) {
        messageCollector.reportErrorOnProperty(
            property = property,
            message = "@ArrayContentBased on property of type <${property.type.render()}> not supported",
        )
        return null
    }

    // Primitive arrays don't need deep equals:
    if (propertyClassifier in context.irBuiltIns.primitiveArraysToPrimitiveTypes) {
        return null
    }

    return findArrayDeepHashCodeFunction(propertyClassifier)
}

context(IrPluginContext)
private fun IrBlockBodyBuilder.findArrayDeepHashCodeFunction(
    propertyClassifier: IrClassifierSymbol,
): IrSimpleFunctionSymbol {
    val callableName = if (propertyClassifier == context.irBuiltIns.arrayClass) {
        "contentDeepHashCode"
    } else {
        "contentHashCode"
    }
    return referenceFunctions(
        callableId = CallableId(
            packageName = FqName("kotlin.collections"),
            callableName = Name.identifier(callableName),
        ),
    ).single { functionSymbol ->
        // Disambiguate against the older non-nullable receiver overload:
        functionSymbol.owner.extensionReceiverParameter?.type?.let {
            it.classifierOrNull == propertyClassifier && it.isNullable()
        } ?: false
    }
}

private fun IrBlockBodyBuilder.getStandardHashCodeFunctionSymbol(
    classifier: IrClassifierSymbol?,
): IrSimpleFunctionSymbol = when {
    classifier.isArrayOrPrimitiveArray(context) ->
        context.irBuiltIns.dataClassArrayMemberHashCodeSymbol
    classifier is IrClassSymbol ->
        getHashCodeFunctionForClass(classifier.owner)
    classifier is IrTypeParameterSymbol ->
        getHashCodeFunctionForClass(classifier.owner.erasedUpperBound)
    else ->
        error("Unknown classifier kind $classifier")
}

private fun IrBlockBodyBuilder.getHashCodeFunctionForClass(
    irClass: IrClass
): IrSimpleFunctionSymbol {
    val explicitHashCodeDeclaration = irClass.functions.singleOrNull {
        it.name.asString() == "hashCode" &&
            it.valueParameters.isEmpty() &&
            it.extensionReceiverParameter == null
    }
    return explicitHashCodeDeclaration?.symbol
        ?: context.irBuiltIns.anyClass.functions.single { it.owner.name.asString() == "hashCode" }
}

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
        valueArgumentsCount = if (hasDispatchReceiver || hasExtensionReceiver) 0 else 1,
        typeArgumentsCount = 0,
    ).apply {
        when {
            hasDispatchReceiver -> dispatchReceiver = value
            hasExtensionReceiver -> extensionReceiver = value
            else -> putValueArgument(0, value)
        }
    }
}

private val IrTypeParameter.erasedUpperBound: IrClass
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
