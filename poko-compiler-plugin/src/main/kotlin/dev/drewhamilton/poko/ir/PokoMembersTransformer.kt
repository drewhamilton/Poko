package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irIfThenReturnFalse
import org.jetbrains.kotlin.ir.builders.irIfThenReturnTrue
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNotIs
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnTrue
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.isMultiFieldValueClass
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.source.getPsi

internal class PokoMembersTransformer(
    private val pokoAnnotationName: ClassId,
    private val pluginContext: IrPluginContext,
    private val messageCollector: MessageCollector,
) : IrElementTransformerVoidWithContext() {

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        log("Reading <$declaration>")

        val declarationParent = declaration.parent
        if (declarationParent is IrClass && declarationParent.isPokoClass() && declaration.isFakeOverride) {
            when {
                declaration.isEquals() -> declaration.convertToGenerated { properties ->
                    generateEqualsMethodBody(declarationParent, declaration, properties)
                }
                declaration.isHashCode() -> declaration.convertToGenerated { properties ->
                    generateHashCodeMethodBody(declaration, properties)
                }
                declaration.isToString() -> declaration.convertToGenerated { properties ->
                    generateToStringMethodBody(declarationParent, declaration, properties)
                }
            }
        }

        return super.visitFunctionNew(declaration)
    }

    private fun IrClass.isPokoClass(): Boolean = when {
        !hasAnnotation(pokoAnnotationName.asSingleFqName()) -> {
            log("Not Poko class")
            false
        }
        isData -> {
            log("Data class")
            reportError("Poko does not support data classes")
            false
        }
        isSingleFieldValueClass || isMultiFieldValueClass -> {
            log("Value class")
            reportError("Poko does not support value classes")
            false
        }
        isInner -> {
            log("Inner class")
            reportError("Poko cannot be applied to inner classes")
            false
        }
        primaryConstructor == null -> {
            log("No primary constructor")
            reportError("Poko classes must have a primary constructor")
            false
        }
        else -> {
            true
        }
    }

    private inline fun IrFunction.convertToGenerated(
        generateFunctionBody: IrBlockBodyBuilder.(List<IrProperty>) -> Unit
    ) {
        val parent = parent as IrClass
        val properties = parent.properties
            .toList()
            .filter {
                // Can't figure out how to check this another way. FIR?
                @OptIn(ObsoleteDescriptorBasedAPI::class)
                it.symbol.descriptor.source.getPsi() is KtParameter
            }
        if (properties.isEmpty()) {
            log("No primary constructor properties")
            parent.reportError("Poko classes must have at least one property in the primary constructor")
            return
        }

        origin = PokoOrigin
        mutateWithNewDispatchReceiverParameterForParentClass()

        body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
            generateFunctionBody(properties)
        }

        reflectivelySetFakeOverride(false)
    }

    //region equals
    private fun IrFunction.isEquals(): Boolean {
        val valueParameters = valueParameters
        return name == Name.identifier("equals") &&
                returnType == pluginContext.irBuiltIns.booleanType &&
                valueParameters.size == 1 && valueParameters[0].type == pluginContext.irBuiltIns.anyNType
    }

    /**
     * Generate the body of the equals method. Adapted from
     * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateEqualsMethodBody].
     */
    private fun IrBlockBodyBuilder.generateEqualsMethodBody(
        irClass: IrClass,
        irFunction: IrFunction,
        irProperties: List<IrProperty>,
    ) {
        val irType = irClass.defaultType
        fun irOther(): IrExpression = IrGetValueImpl(irFunction.valueParameters[0])

        if (!irClass.isSingleFieldValueClass) {
            +irIfThenReturnTrue(irEqeqeq(receiver(irFunction), irOther()))
        }
        +irIfThenReturnFalse(irNotIs(irOther(), irType))
        val otherWithCast = irTemporary(irAs(irOther(), irType), "other_with_cast")
        for (property in irProperties) {
            val field = property.backingField!!
            val arg1 = irGetField(receiver(irFunction), field)
            val arg2 = irGetField(irGet(irType, otherWithCast.symbol), field)
            val negativeComparison = when {
                property.hasAnnotation(ArrayContentBasedAnnotation.asSingleFqName()) -> {
                    irNot(
                        irArrayContentDeepEquals(
                            receiver = arg1,
                            argument = arg2,
                            irProperty = property,
                        ),
                    )
                }

                else -> {
                    irNotEquals(arg1, arg2)
                }
            }
            +irIfThenReturnFalse(negativeComparison)
        }
        +irReturnTrue()
    }

    private fun IrBuilderWithScope.irArrayContentDeepEquals(
        receiver: IrExpression,
        argument: IrExpression,
        irProperty: IrProperty,
    ): IrExpression {
        val propertyType = irProperty.type
        val propertyClassifier = propertyType.classifierOrFail

        if (!propertyClassifier.isArrayOrPrimitiveArray(context)) {
            // TODO: Handle generic type that is an array at runtime
            return if (propertyClassifier == context.irBuiltIns.anyClass) {
                irRuntimeArrayContentDeepEquals(receiver, argument)
            } else {
                irProperty.reportError(
                    "@ArrayContentBased on property of type <${propertyType.render()}> not supported"
                )
                irEquals(receiver, argument)
            }
        }

        return irCall(
            findContentDeepEqualsFunctionSymbol(propertyClassifier),
            type = context.irBuiltIns.booleanType,
            valueArgumentsCount = 1,
            typeArgumentsCount = 1,
        ).apply {
            extensionReceiver = receiver
            putValueArgument(0, argument)
        }
    }

    private fun IrBuilderWithScope.irRuntimeArrayContentDeepEquals(
        receiver: IrExpression,
        argument: IrExpression,
    ): IrExpression {
        val starArrayType = context.irBuiltIns.arrayClass.createType(
            hasQuestionMark = false,
            arguments = listOf(IrStarProjectionImpl),
        )
        return irComposite {
            +irWhen(
                type = context.irBuiltIns.unitType,
                branches = listOf(
                    irBranch(
                        condition = irIs(
                            argument = receiver,
                            type = starArrayType,
                        ),
                        result = irIfThenReturnFalse(
                            irCall(
                                callee = context.irBuiltIns.ororSymbol,
                                type = context.irBuiltIns.booleanType,
                                valueArgumentsCount = 2,
                            ).apply {
                                putValueArgument(0, irNotIs(argument, starArrayType))
                                putValueArgument(
                                    index = 1,
                                    valueArgument = irNot(
                                        irCall(
                                            callee = findContentDeepEqualsFunctionSymbol(
                                                context.irBuiltIns.arrayClass,
                                            ),
                                            type = context.irBuiltIns.booleanType,
                                            valueArgumentsCount = 1,
                                            typeArgumentsCount = 1,
                                        ).apply {
                                            extensionReceiver = receiver
                                            putValueArgument(0, argument)
                                        }
                                    )
                                )
                            },
                        ),
                    ),

                    // TODO: Primitive arrays

                    irElseBranch(
                        irIfThenReturnFalse(irNotEquals(receiver, argument)),
                    ),
                ),
            )
            +irReturnTrue()
        }
    }

    /**
     * Finds `contentDeepEquals` function if [propertyClassifier] is a typed array, or
     * `contentEquals` function if it is a primitive array.
     */
    private fun IrBuilderWithScope.findContentDeepEqualsFunctionSymbol(
        propertyClassifier: IrClassifierSymbol,
    ): IrSimpleFunctionSymbol {
        val callableName = if (propertyClassifier == context.irBuiltIns.arrayClass) {
            "contentDeepEquals"
        } else {
            "contentEquals"
        }
        return pluginContext.referenceFunctions(
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
    //endregion

    //region hashCode
    private fun IrFunction.isHashCode(): Boolean =
        name == Name.identifier("hashCode") &&
                returnType == pluginContext.irBuiltIns.intType &&
                valueParameters.isEmpty()

    /**
     * Generate the body of the hashCode method. Adapted from
     * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateHashCodeMethodBody].
     */
    private fun IrBlockBodyBuilder.generateHashCodeMethodBody(
        irFunction: IrFunction,
        irProperties: List<IrProperty>,
    ) {
        if (irProperties.isEmpty()) {
            +irReturn(irInt(0))
            return
        } else if (irProperties.size == 1) {
            +irReturn(getHashCodeOfProperty(irFunction, irProperties[0]))
            return
        }

        val irIntType = context.irBuiltIns.intType

        val irResultVar = IrVariableImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.DEFINED,
            IrVariableSymbolImpl(),
            Name.identifier("result"), irIntType,
            isVar = true, isConst = false, isLateinit = false
        ).also {
            it.parent = irFunction
            it.initializer = getHashCodeOfProperty(irFunction, irProperties[0])
        }
        +irResultVar

        for (property in irProperties.drop(1)) {
            val shiftedResult = irCallOp(
                callee = context.irBuiltIns.intTimesSymbol,
                type = irIntType,
                dispatchReceiver = irGet(irResultVar),
                argument = irInt(31),
            )
            val irRhs = irCallOp(context.irBuiltIns.intPlusSymbol, irIntType, shiftedResult, getHashCodeOfProperty(irFunction, property))
            +irSet(irResultVar.symbol, irRhs)
        }

        +irReturn(irGet(irResultVar))
    }

    private fun IrBlockBodyBuilder.getHashCodeOfProperty(
        irFunction: IrFunction,
        irProperty: IrProperty,
    ): IrExpression {
        val field = irProperty.backingField!!
        return when {
            irProperty.type.isNullable() -> irIfNull(
                context.irBuiltIns.intType,
                irGetField(receiver(irFunction), field),
                irInt(0),
                getHashCodeOf(irProperty, irGetField(receiver(irFunction), field))
            )
            else -> getHashCodeOf(irProperty, irGetField(receiver(irFunction), field))
        }
    }

    /**
     * Symbol-retrieval adapted from [org.jetbrains.kotlin.fir.backend.generators.DataClassMembersGenerator].
     */
    private fun IrBlockBodyBuilder.getHashCodeOf(
        property: IrProperty,
        irValue: IrExpression,
    ): IrExpression {
        val hasArrayContentBasedAnnotation =
            property.hasAnnotation(ArrayContentBasedAnnotation.asSingleFqName())
        val classifier = property.type.classifierOrNull
        // TODO: Handle generic type property that is array at runtime
        val mayBeRuntimeArray = classifier == context.irBuiltIns.anyClass

        if (hasArrayContentBasedAnnotation && mayBeRuntimeArray) {
            return irRuntimeArrayContentDeepHashCode(irValue)
        }

        // Non-null if deep hashCode will be used, else null:
        val deepHashCodeFunctionSymbol = if (hasArrayContentBasedAnnotation) {
            maybeFindArrayDeepHashCodeFunction(property)
        } else {
            null
        }
        val hashCodeFunctionSymbol = deepHashCodeFunctionSymbol
            ?: getDataClassHashCodeFunction(classifier)
        check(hashCodeFunctionSymbol.isBound) { "$hashCodeFunctionSymbol is not bound" }

        // Poko modification: check for extension receiver for contentDeepHashCode case
        val (hasDispatchReceiver, hasExtensionReceiver) = with(hashCodeFunctionSymbol.owner) {
            (dispatchReceiverParameter != null) to (extensionReceiverParameter != null)
        }
        return irCall(
            hashCodeFunctionSymbol,
            context.irBuiltIns.intType,
            valueArgumentsCount = if (hasDispatchReceiver || hasExtensionReceiver) 0 else 1,
            typeArgumentsCount = 0
        ).apply {
            when {
                hasDispatchReceiver -> dispatchReceiver = irValue
                hasExtensionReceiver -> extensionReceiver = irValue
                else -> putValueArgument(0, irValue)
            }
        }
    }

    /**
     * Invokes a `when` branch that checks the runtime type of the [property] instance and invokes
     * `contentDeepHashCode` or `contentHashCode` for typed arrays and primitive arrays,
     * respectively.
     */
    private fun IrBlockBodyBuilder.irRuntimeArrayContentDeepHashCode(
        property: IrExpression,
    ): IrExpression {
        val starArrayType = context.irBuiltIns.arrayClass.createType(
            hasQuestionMark = false,
            arguments = listOf(IrStarProjectionImpl),
        )
        return irWhen(
            type = context.irBuiltIns.intType,
            branches = listOf(
                irBranch(
                    condition = irIs(
                        argument = property,
                        type = starArrayType,
                    ),
                    result = irCall(
                        callee = findArrayDeepHashCodeFunction(context.irBuiltIns.arrayClass),
                        type = context.irBuiltIns.intType,
                    ).apply {
                        extensionReceiver = property
                    }
                ),

                // TODO: Primitive arrays

                irElseBranch(
                    irIfNull(
                        type = context.irBuiltIns.intType,
                        subject = property,
                        thenPart = irInt(0),
                        elsePart = irComposite {
                            // TODO: Deduplicate?
                            val hashCodeFunctionSymbol = getDataClassHashCodeFunction(
                                classifier = property.type.classifierOrNull,
                            )
                            check(hashCodeFunctionSymbol.isBound) {
                                "$hashCodeFunctionSymbol is not bound"
                            }

                            // Poko modification: check for extension receiver for
                            // contentDeepHashCode case
                            val hasDispatchReceiver =
                                hashCodeFunctionSymbol.owner.dispatchReceiverParameter != null
                            +irCall(
                                hashCodeFunctionSymbol,
                                context.irBuiltIns.intType,
                                valueArgumentsCount = if (hasDispatchReceiver) 0 else 1,
                                typeArgumentsCount = 0
                            ).apply {
                                when {
                                    hasDispatchReceiver -> dispatchReceiver = property
                                    else -> putValueArgument(0, property)
                                }
                            }
                        },
                    ),
                ),
            ),
        )
    }

    /**
     * Returns contentDeepHashCode function symbol if it is an appropriate option for [irProperty],
     * else returns null.
     */
    private fun IrBlockBodyBuilder.maybeFindArrayDeepHashCodeFunction(
        irProperty: IrProperty,
    ): IrSimpleFunctionSymbol? {
        val propertyClassifier = irProperty.type.classifierOrFail

        if (!propertyClassifier.isArrayOrPrimitiveArray(context)) {
            irProperty.reportError(
                "@ArrayContentBased on property of type <${irProperty.type.render()}> not supported"
            )
            return null
        }

        // Primitive arrays don't need deep equals:
        if (propertyClassifier in context.irBuiltIns.primitiveArraysToPrimitiveTypes) {
            return null
        }

        return findArrayDeepHashCodeFunction(propertyClassifier)
    }

    private fun IrBlockBodyBuilder.findArrayDeepHashCodeFunction(
        propertyClassifier: IrClassifierSymbol,
    ): IrSimpleFunctionSymbol {
        val callableName = if (propertyClassifier == context.irBuiltIns.arrayClass) {
            "contentDeepHashCode"
        } else {
            "contentHashCode"
        }
        return pluginContext.referenceFunctions(
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

    private fun IrBlockBodyBuilder.getDataClassHashCodeFunction(
        classifier: IrClassifierSymbol?,
    ): IrSimpleFunctionSymbol = when {
        classifier.isArrayOrPrimitiveArray(context) ->
            context.irBuiltIns.dataClassArrayMemberHashCodeSymbol
        classifier is IrClassSymbol ->
            getHashCodeFunction(classifier.owner)
        classifier is IrTypeParameterSymbol ->
            getHashCodeFunction(classifier.owner.erasedUpperBound)
        else ->
            error("Unknown classifier kind $classifier")
    }

    private fun IrBlockBodyBuilder.getHashCodeFunction(irClass: IrClass): IrSimpleFunctionSymbol {
        return irClass.functions.singleOrNull {
            it.name.asString() == "hashCode" && it.valueParameters.isEmpty() && it.extensionReceiverParameter == null
        }?.symbol ?: context.irBuiltIns.anyClass.functions.single { it.owner.name.asString() == "hashCode" }
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
    //endregion

    //region toString
    private fun IrFunction.isToString(): Boolean =
        name == Name.identifier("toString") &&
                returnType == pluginContext.irBuiltIns.stringType &&
                valueParameters.isEmpty()

    /**
     * Generate the body of the toString method. Adapted from
     * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateToStringMethodBody].
     */
    private fun IrBlockBodyBuilder.generateToStringMethodBody(
        irClass: IrClass,
        irFunction: IrFunction,
        irProperties: List<IrProperty>,
    ) {
        val irConcat = irConcat()
        irConcat.addArgument(irString(irClass.name.asString() + "("))
        var first = true
        for (property in irProperties) {
            if (!first) irConcat.addArgument(irString(", "))

            irConcat.addArgument(irString(property.name.asString() + "="))

            val irPropertyValue = irGetField(receiver(irFunction), property.backingField!!)

            val classifier = property.type.classifierOrNull
            val hasArrayContentBasedAnnotation =
                property.hasAnnotation(ArrayContentBasedAnnotation.asSingleFqName())
            // TODO: Handle generic type property that is array at runtime
            val mayBeRuntimeArray = classifier == context.irBuiltIns.anyClass
            val irPropertyStringValue = when {
                hasArrayContentBasedAnnotation && mayBeRuntimeArray -> {
                    val field = property.backingField!!
                    val instance = irGetField(receiver(irFunction), field)
                    irRuntimeArrayContentDeepToString(instance)
                }

                hasArrayContentBasedAnnotation -> {
                    val toStringFunctionSymbol = maybeFindArrayDeepToStringFunction(property)
                        ?: context.irBuiltIns.dataClassArrayMemberToStringSymbol
                    irCall(
                        callee = toStringFunctionSymbol,
                        type = context.irBuiltIns.stringType,
                    ).apply {
                        // Poko modification: check for extension receiver for contentDeepToString
                        val hasExtensionReceiver =
                            toStringFunctionSymbol.owner.extensionReceiverParameter != null
                        if (hasExtensionReceiver) {
                            extensionReceiver = irPropertyValue
                        } else {
                            putValueArgument(0, irPropertyValue)
                        }
                    }
                }

                classifier.isArrayOrPrimitiveArray(context) -> {
                    irCall(
                        callee = context.irBuiltIns.dataClassArrayMemberToStringSymbol,
                        type = context.irBuiltIns.stringType
                    ).apply {
                        putValueArgument(0, irPropertyValue)
                    }
                }

                else -> irPropertyValue
            }

            irConcat.addArgument(irPropertyStringValue)
            first = false
        }
        irConcat.addArgument(irString(")"))
        +irReturn(irConcat)
    }

    /**
     * Returns `contentDeepToString` function symbol if it is an appropriate option for
     * [irProperty], else returns null.
     */
    private fun IrBlockBodyBuilder.maybeFindArrayDeepToStringFunction(
        irProperty: IrProperty,
    ): IrSimpleFunctionSymbol? {
        val propertyClassifier = irProperty.type.classifierOrFail

        if (!propertyClassifier.isArrayOrPrimitiveArray(context)) {
            irProperty.reportError(
                "@ArrayContentBased on property of type <${irProperty.type.render()}> not supported"
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
     * Invokes a `when` branch that checks the runtime type of the [property] instance and invokes
     * `contentDeepToString` or `contentToString` for typed arrays and primitive arrays,
     * respectively.
     */
    private fun IrBlockBodyBuilder.irRuntimeArrayContentDeepToString(
        property: IrExpression,
    ): IrExpression {
        val starArrayType = context.irBuiltIns.arrayClass.createType(
            hasQuestionMark = false,
            arguments = listOf(IrStarProjectionImpl),
        )
        return irWhen(
            type = context.irBuiltIns.stringType,
            branches = listOf(
                irBranch(
                    condition = irIs(
                        argument = property,
                        type = starArrayType,
                    ),
                    result = irCall(
                        callee = findContentDeepToStringFunctionSymbol(
                            context.irBuiltIns.arrayClass,
                        ),
                        type = context.irBuiltIns.stringType,
                    ).apply {
                        extensionReceiver = property
                    }
                ),

                // TODO: Primitive arrays

                irElseBranch(
                    irCall(
                        callee = context.irBuiltIns.extensionToString,
                        type = context.irBuiltIns.stringType,
                    ).apply {
                        extensionReceiver = property
                    }
                ),
            ),
        )
    }

    /**
     * Finds `contentDeepToString` function if [propertyClassifier] is a typed array, or
     * `contentToString` function if it is a primitive array.
     */
    private fun IrBuilderWithScope.findContentDeepToStringFunctionSymbol(
        propertyClassifier: IrClassifierSymbol,
    ): IrSimpleFunctionSymbol {
        val callableName = if (propertyClassifier == context.irBuiltIns.arrayClass) {
            "contentDeepToString"
        } else {
            "contentToString"
        }
        return pluginContext.referenceFunctions(
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
    //endregion

    //region Shared generation helpers
    /**
     * Converts the function's dispatch receiver parameter (i.e. <this>) to the function's parent.
     * This is necessary because we are taking the base declaration from a parent class (or Any) and
     * pseudo-overriding it in this function's parent class.
     */
    private fun IrFunction.mutateWithNewDispatchReceiverParameterForParentClass() {
        val parentClass = parent as IrClass
        val originalReceiver = checkNotNull(dispatchReceiverParameter)
        dispatchReceiverParameter = IrValueParameterImpl(
            startOffset = originalReceiver.startOffset,
            endOffset = originalReceiver.endOffset,
            origin = originalReceiver.origin,
            symbol = IrValueParameterSymbolImpl(
                // IrValueParameterSymbolImpl requires a descriptor; same type as
                // originalReceiver.symbol:
                @OptIn(ObsoleteDescriptorBasedAPI::class)
                LazyClassReceiverParameterDescriptor(parentClass.descriptor)
            ),
            name = originalReceiver.name,
            index = originalReceiver.index,
            type = parentClass.symbol.createType(hasQuestionMark = false, emptyList()),
            varargElementType = originalReceiver.varargElementType,
            isCrossinline = originalReceiver.isCrossinline,
            isNoinline = originalReceiver.isNoinline,
            isHidden = originalReceiver.isHidden,
            isAssignable = originalReceiver.isAssignable
        ).apply {
            parent = this@mutateWithNewDispatchReceiverParameterForParentClass
        }
    }

    /**
     * Only works properly after [mutateWithNewDispatchReceiverParameterForParentClass] has been
     * called on [irFunction].
     */
    private fun IrBlockBodyBuilder.receiver(irFunction: IrFunction) =
        IrGetValueImpl(irFunction.dispatchReceiverParameter!!)

    private fun IrBlockBodyBuilder.IrGetValueImpl(irParameter: IrValueParameter) = IrGetValueImpl(
        startOffset, endOffset,
        irParameter.type,
        irParameter.symbol
    )

    private fun IrFunction.reflectivelySetFakeOverride(isFakeOverride: Boolean) {
        with(javaClass.getDeclaredField("isFakeOverride")) {
            isAccessible = true
            setBoolean(this@reflectivelySetFakeOverride, isFakeOverride)
        }
    }

    private val IrProperty.type
        get() = this.backingField?.type
            ?: this.getter?.returnType
            ?: error("Can't find type of ${this.render()}")

    private fun IrClassifierSymbol?.isArrayOrPrimitiveArray(context: IrGeneratorContext): Boolean {
        return this == context.irBuiltIns.arrayClass || this in context.irBuiltIns.primitiveArraysToPrimitiveTypes
    }
    //endregion

    private fun log(message: String) {
        messageCollector.report(CompilerMessageSeverity.LOGGING, "POKO COMPILER PLUGIN (IR): $message")
    }

    private fun IrClass.reportError(message: String) {
        val psi = source.getPsi()
        val location = MessageUtil.psiElementToMessageLocation(psi)
        messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
    }

    // TODO: Implement an FIR-based declaration checker:
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrProperty.reportError(message: String) {
        val psi = descriptor.source.getPsi()
        val location = MessageUtil.psiElementToMessageLocation(psi)
        messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
    }

    private companion object {
        val ArrayContentBasedAnnotation = ClassId(
            FqName("dev.drewhamilton.poko"),
            Name.identifier("ArrayContentBased"),
        )
    }
}
