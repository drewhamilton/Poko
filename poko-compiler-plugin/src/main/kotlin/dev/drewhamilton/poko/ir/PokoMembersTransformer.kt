package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irIfThenReturnFalse
import org.jetbrains.kotlin.ir.builders.irIfThenReturnTrue
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNotIs
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnTrue
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class PokoMembersTransformer(
    private val annotationClass: IrClassSymbol,
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
        !hasAnnotation(annotationClass) -> {
            log("Not Poko class")
            false
        }
        isData -> {
            log("Data class")
            reportError("Poko does not support data classes")
            false
        }
        isInline -> {
            log("Inline class")
            reportError("Poko does not support inline classes")
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
            .filter { it.symbol.descriptor.source.getPsi() is KtParameter }
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
     * Generate the body of the equals method. Copied from
     * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateEqualsMethodBody].
     */
    private fun IrBlockBodyBuilder.generateEqualsMethodBody(
        irClass: IrClass,
        irFunction: IrFunction,
        irProperties: List<IrProperty>,
    ) {
        val irType = irClass.defaultType
        fun irOther(): IrExpression = IrGetValueImpl(irFunction.valueParameters[0])

        if (!irClass.isInline) {
            +irIfThenReturnTrue(irEqeqeq(receiver(irFunction), irOther()))
        }
        +irIfThenReturnFalse(irNotIs(irOther(), irType))
        val otherWithCast = irTemporary(irAs(irOther(), irType), "other_with_cast")
        for (property in irProperties) {
            val field = property.backingField!!
            val arg1 = irGetField(receiver(irFunction), field)
            val arg2 = irGetField(irGet(irType, otherWithCast.symbol), field)
            +irIfThenReturnFalse(irNotEquals(arg1, arg2))
        }
        +irReturnTrue()
    }
    //endregion

    //region hashCode
    private fun IrFunction.isHashCode(): Boolean =
        name == Name.identifier("hashCode") &&
                returnType == pluginContext.irBuiltIns.intType &&
                valueParameters.isEmpty()

    /**
     * Generate the body of the hashCode method. Copied from
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

        val intClass = pluginContext.irBuiltIns.intClass
        val intTimesSymbol: IrSimpleFunctionSymbol = intClass.getSimpleFunction("times")!!
        val intPlusSymbol: IrSimpleFunctionSymbol = intClass.getSimpleFunction("plus")!!
        for (property in irProperties.drop(1)) {
            val shiftedResult = irCallOp(intTimesSymbol, irIntType, irGet(irResultVar), irInt(31))
            val irRhs = irCallOp(intPlusSymbol, irIntType, shiftedResult, getHashCodeOfProperty(irFunction, property))
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
            irProperty.descriptor.type.isNullable() -> irIfNull(
                context.irBuiltIns.intType,
                irGetField(receiver(irFunction), field),
                irInt(0),
                getHashCodeOf(irProperty, irGetField(receiver(irFunction), field))
            )
            else -> getHashCodeOf(irProperty, irGetField(receiver(irFunction), field))
        }
    }

    private fun IrBlockBodyBuilder.getHashCodeOf(property: IrProperty, irValue: IrExpression): IrExpression {
        val hashCodeFunctionSymbol = getHashCodeFunction(property) { descriptor ->
            pluginContext.referenceFunctions(descriptor.fqNameSafe).first().also {
                require(it.isBound) { "$it is not bound" }
            }
        }

        val hasDispatchReceiver = hashCodeFunctionSymbol.descriptor.dispatchReceiverParameter != null
        return irCall(
            hashCodeFunctionSymbol,
            context.irBuiltIns.intType,
            valueArgumentsCount = if (hasDispatchReceiver) 0 else 1,
            typeArgumentsCount = 0
        ).apply {
            if (hasDispatchReceiver) {
                dispatchReceiver = irValue
            } else {
                putValueArgument(0, irValue)
            }
        }
    }

    private fun IrBlockBodyBuilder.getHashCodeFunction(type: KotlinType): FunctionDescriptor =
        type.memberScope.findHashCodeFunctionOrNull()
            ?: context.irBuiltIns.anyClass.descriptor.unsubstitutedMemberScope.findHashCodeFunctionOrNull()!!

    private fun IrBlockBodyBuilder.getHashCodeFunction(
        type: KotlinType,
        symbolResolve: (FunctionDescriptor) -> IrSimpleFunctionSymbol
    ): IrSimpleFunctionSymbol =
        when (val typeConstructorDescriptor = type.constructor.declarationDescriptor) {
            is ClassDescriptor ->
                if (KotlinBuiltIns.isArrayOrPrimitiveArray(typeConstructorDescriptor))
                    context.irBuiltIns.dataClassArrayMemberHashCodeSymbol
                else
                    symbolResolve(getHashCodeFunction(type))

            is TypeParameterDescriptor ->
                getHashCodeFunction(typeConstructorDescriptor.representativeUpperBound, symbolResolve)

            else -> throw AssertionError("Unexpected type: $type")
        }

    private fun IrBlockBodyBuilder.getHashCodeFunction(
        property: IrProperty,
        symbolResolve: (FunctionDescriptor) -> IrSimpleFunctionSymbol
    ): IrSimpleFunctionSymbol = getHashCodeFunction(property.descriptor.type, symbolResolve)

    private fun MemberScope.findHashCodeFunctionOrNull() =
        getContributedFunctions(Name.identifier("hashCode"), NoLookupLocation.FROM_BACKEND)
            .find { it.valueParameters.isEmpty() }
    //endregion

    //region toString
    private fun IrFunction.isToString(): Boolean =
        name == Name.identifier("toString") &&
                returnType == pluginContext.irBuiltIns.stringType &&
                valueParameters.isEmpty()

    /**
     * Generate the body of the toString method. Copied from
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

            val typeConstructorDescriptor = property.descriptor.type.constructor.declarationDescriptor
            val irPropertyStringValue = if (
                typeConstructorDescriptor is ClassDescriptor &&
                KotlinBuiltIns.isArrayOrPrimitiveArray(typeConstructorDescriptor)
            ) {
                irCall(context.irBuiltIns.dataClassArrayMemberToStringSymbol, context.irBuiltIns.stringType).apply {
                    putValueArgument(0, irPropertyValue)
                }
            } else {
                irPropertyValue
            }

            irConcat.addArgument(irPropertyStringValue)
            first = false
        }
        irConcat.addArgument(irString(")"))
        +irReturn(irConcat)
    }
    //endregion

    //region Shared generation helpers
    private fun IrFunction.mutateWithNewDispatchReceiverParameterForParentClass() {
        val parentClass = parent as IrClass
        val originalReceiver = checkNotNull(dispatchReceiverParameter)
        dispatchReceiverParameter = IrValueParameterImpl(
            startOffset = originalReceiver.startOffset,
            endOffset = originalReceiver.endOffset,
            origin = originalReceiver.origin,
            symbol = IrValueParameterSymbolImpl(LazyClassReceiverParameterDescriptor(parentClass.descriptor)),
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
     * Only works properly after [mutateWithNewDispatchReceiverParameterForParentClass] has been called on [irFunction].
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
    //endregion

    private fun log(message: String) {
        messageCollector.report(CompilerMessageSeverity.LOGGING, "POKO COMPILER PLUGIN (IR): $message")
    }

    private fun IrClass.reportError(message: String) {
        val psi = descriptor.source.getPsi()
        val location = MessageUtil.psiElementToMessageLocation(psi)
        messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
    }
}
