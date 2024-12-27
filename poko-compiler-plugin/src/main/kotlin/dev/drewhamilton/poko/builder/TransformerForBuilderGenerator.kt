/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package dev.drewhamilton.poko.builder

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.nestedClasses
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class TransformerForBuilderGenerator(
    context: IrPluginContext,
) : AbstractTransformerForGenerator(context, visitBodies = false) {

    override fun interestedIn(
        key: GeneratedDeclarationKey?,
    ): Boolean {
        return key == BuilderFirDeclarationGenerationExtension.Key
    }

    override fun generateBodyForFunction(
        function: IrSimpleFunction,
        key: GeneratedDeclarationKey?,
    ): IrBody? {
        return when {
            function.isBuilderSetter() -> generateSetterFunctionBody(function)
            function.isBuildFunction() -> generateBuildFunctionBody(function)
            function.isFactoryFunction() -> generateFactoryFunctionBody(function)
            else -> null
        }
    }

    private fun IrFunction.isBuilderSetter(): Boolean {
        return extensionReceiverParameter == null &&
            typeParameters.isEmpty() &&
            valueParameters.size == 1 &&
            returnType.classOrNull?.owner == parent
    }

    private fun generateSetterFunctionBody(
        function: IrFunction,
    ): IrBody {
        val param = function.valueParameters.single()
        val parentClassInstance = function.parentClassInstance()
        val setFieldStatement = IrSetFieldImpl(
            startOffset = -1,
            endOffset = -1,
            symbol = function.parentAsClass
                .properties
                .single { it.name == param.name }
                .backingField!!
                .symbol,
            receiver = parentClassInstance,
            value = IrGetValueImpl(
                startOffset = -1,
                endOffset = -1,
                type = param.type,
                symbol = param.symbol,
            ),
            type = irBuiltIns.unitType, // setter return type
        )
        val returnStatement = IrReturnImpl(
            startOffset = -1,
            endOffset = -1,
            type = function.returnType,
            returnTargetSymbol = function.symbol,
            value = parentClassInstance,
        )
        return irFactory.createBlockBody(
            startOffset = -1,
            endOffset = -1,
            statements = listOf(
                setFieldStatement,
                returnStatement,
            ),
        )
    }

    private fun IrFunction.isBuildFunction(): Boolean {
        return name == BuildFunctionIdentifierName &&
            extensionReceiverParameter == null &&
            typeParameters.isEmpty() &&
            valueParameters.isEmpty()
    }

    private fun generateBuildFunctionBody(
        function: IrFunction,
    ): IrBody {
        val builderClass = function.parentAsClass
        val builderFields = builderClass.properties.map { it.backingField!! }
        val pokoClass = builderClass.parentAsClass
        val pokoClassConstructor = pokoClass.primaryConstructor!!

        val constructorInvocation = IrConstructorCallImpl(
            startOffset = -1,
            endOffset = -1,
            type = pokoClass.defaultType,
            symbol = pokoClassConstructor.symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        ).apply {
            // Builder property backing fields should match Poko class constructor parameter names:
            pokoClassConstructor.valueParameters.forEachIndexed { index, parameter ->
                val builderField = try {
                    builderFields.single { it.name == parameter.name }
                } catch (noSuchElementException: NoSuchElementException) {
                    throw IllegalStateException(
                        "Could not find field named <${parameter.name}> in ${builderFields.toList()}",
                        noSuchElementException,
                    )
                }
                putValueArgument(
                    index = index,
                    valueArgument = IrGetFieldImpl(
                        startOffset = -1,
                        endOffset = -1,
                        symbol = builderField.symbol,
                        type = builderField.type,
                        receiver = function.parentClassInstance(),
                    ),
                )
            }
        }

        return irFactory.createBlockBody(
            startOffset = -1,
            endOffset = -1,
            statements = listOf(
                IrReturnImpl(
                    startOffset = -1,
                    endOffset = -1,
                    type = function.returnType,
                    returnTargetSymbol = function.symbol,
                    value = constructorInvocation,
                ),
            ),
        )
    }

    private fun IrFunction.isFactoryFunction(): Boolean {
        return isTopLevel &&
            extensionReceiverParameter == null &&
            typeParameters.isEmpty() &&
            valueParameters.size == 1 &&
            name == returnType.classFqName?.shortName() &&
            valueParameters.single().let { param ->
                val type = (param.type as IrSimpleTypeImpl)
                type == irBuiltIns.functionN(1).typeWith(type.arguments.map { it.typeOrFail })
            }
    }

    private fun generateFactoryFunctionBody(
        function: IrFunction,
    ): IrBody {
        val builderClass = function.returnType
            .getClass()!!
            .nestedClasses
            .single { it.name == BuilderClassName }
        val builderClassConstructor =  builderClass.primaryConstructor!!

        val builderConstructorInvocation = IrConstructorCallImpl(
            startOffset = -1,
            endOffset = -1,
            type = builderClass.defaultType,
            symbol = builderClassConstructor.symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        )
        val builderVariableInstantiation = IrVariableImpl(
            startOffset = -1,
            endOffset = -1,
            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            symbol = IrVariableSymbolImpl(),
            name = Name.identifier("builder"),
            type = builderClass.defaultType,
            isVar = false,
            isConst = false,
            isLateinit = false,
        ).apply {
            parent = function
            initializer = builderConstructorInvocation
        }

        val invokeCall = irBuiltIns.functionN(1)
            .functions
            .single { it.name.identifier == "invoke" }
        val initializerInvocation = IrCallImpl(
            startOffset = -1,
            endOffset = -1,
            type = irBuiltIns.unitType,
            symbol = invokeCall.symbol,
            typeArgumentsCount = 0,
        ).apply {
        }

        val buildCall = builderClass.functions.single { it.name == BuildFunctionIdentifierName }

        return irFactory.createBlockBody(
            startOffset = -1,
            endOffset = -1,
            statements = listOf(
                IrReturnImpl(
                    startOffset = -1,
                    endOffset = -1,
                    type = function.returnType,
                    returnTargetSymbol = function.symbol,
                    value = IrCallImpl(
                        startOffset = -1,
                        endOffset = -1,
                        type = function.returnType,
                        symbol = buildCall.symbol,
                    ).apply {
                        dispatchReceiver = IrCallImpl(
                            startOffset = -1,
                            endOffset = -1,
                            type = builderClass.defaultType,
                            symbol = IrSimpleFunctionSymbolImpl(
                                descriptor = null,
                                // FIXME: Doesn't work
                                signature = IdSignature.CommonSignature(
                                    packageFqName = "kotlin",
                                    declarationFqName = "apply",
                                    id = null,
                                    mask = 0L,
                                    description = null,
                                ),
                            ),
                        ).apply {
                            extensionReceiver = IrConstructorCallImpl(
                                startOffset = -1,
                                endOffset = -1,
                                type = builderClass.defaultType,
                                symbol = builderClassConstructor.symbol,
                                typeArgumentsCount = 0,
                                constructorTypeArgumentsCount = 0,
                            )
                            putValueArgument(
                                index = 0,
                                valueArgument = IrGetValueImpl(
                                    startOffset = -1,
                                    endOffset = -1,
                                    type = irBuiltIns.functionN(1).defaultType,
                                    symbol = function.valueParameters.single().symbol,
                                ),
                            )
                        }
                    },
                )
            ),
        )
    }

    private fun IrFunction.parentClassInstance(): IrGetValueImpl {
        return IrGetValueImpl(
            startOffset = -1,
            endOffset = -1,
            type = dispatchReceiverParameter!!.type,
            symbol = dispatchReceiverParameter!!.symbol,
        )
    }

    // Adapted from https://github.com/JetBrains/kotlin/blob/52a3ec9184fa44b2c8ce981f279cd66686dbe73b/plugins/plugin-sandbox/src/org/jetbrains/kotlin/plugin/sandbox/ir/AbstractTransformerForGenerator.kt#L89-L108
    override fun generateBodyForConstructor(
        constructor: IrConstructor,
        key: GeneratedDeclarationKey?,
    ): IrBody? {
        val type = constructor.returnType as? IrSimpleType ?: return null

        val delegatingAnyCall = IrDelegatingConstructorCallImpl(
            startOffset = -1,
            endOffset = -1,
            type = irBuiltIns.anyType,
            symbol = irBuiltIns.anyClass.owner.primaryConstructor?.symbol ?: return null,
            typeArgumentsCount = 0,
        )

        val initializerCall = IrInstanceInitializerCallImpl(
            startOffset = -1,
            endOffset = -1,
            classSymbol = (constructor.parent as? IrClass)?.symbol ?: return null,
            type = type,
        )

        return irFactory.createBlockBody(
            startOffset = -1,
            endOffset = -1,
            statements = listOf(delegatingAnyCall, initializerCall),
        )
    }
}
