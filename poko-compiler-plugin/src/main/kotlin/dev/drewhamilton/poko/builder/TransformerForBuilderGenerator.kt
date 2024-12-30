/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package dev.drewhamilton.poko.builder

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.nestedClasses
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class TransformerForBuilderGenerator(
    private val context: IrPluginContext,
) : IrElementVisitorVoid {

    private val irBuiltIns = context.irBuiltIns

    //region Functions
    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val origin = declaration.origin
        if (origin !is GeneratedByPlugin || !interestedIn(origin.pluginKey)) {
            return
        }

        require(declaration.body == null)
        declaration.body = when {
            declaration.isBuilderSetter() -> generateSetterFunctionBody(declaration)
            declaration.isBuildFunction() -> generateBuildFunctionBody(declaration)
            declaration.isFactoryFunction() -> generateFactoryFunctionBody(declaration)
            else -> null
        }
    }

    //region Builder setter
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

        return DeclarationIrBuilder(
            generatorContext = context,
            symbol = function.symbol,
        ).irBlockBody {
            +irSetField(
                receiver = parentClassInstance,
                field = function.parentAsClass
                    .properties
                    .single { it.name == param.name }
                    .backingField!!,
                value = irGet(param),
            )
            +irReturn(
                value = parentClassInstance,
            )
        }
    }
    //endregion

    //region Builder build
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

        return DeclarationIrBuilder(
            generatorContext = context,
            symbol = function.symbol,
        ).irBlockBody {
            +irReturn(
                value = irCall(
                    callee = pokoClassConstructor.symbol,
                ).apply {
                    // Builder property backing field names should match Poko class constructor
                    // parameter names:
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
                            valueArgument = irGetField(
                                type = builderField.type,
                                receiver = function.parentClassInstance(),
                                field = builderField,
                            ),
                        )
                    }
                }
            )
        }
    }
    //endregion

    //region Top-level factory
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
        val builderLambda = function.valueParameters.single()
        val buildCall = builderClass.functions.single { it.name == BuildFunctionIdentifierName }

        return DeclarationIrBuilder(
            generatorContext = context,
            symbol = function.symbol,
        ).irBlockBody {
            val temp = irTemporary(
                value = irCall(builderClassConstructor),
                nameHint = "builder",
            )

            +irCall(
                callee = irBuiltIns.functionN(1)
                    .functions
                    .single { it.name.identifier == "invoke" },
            ).apply {
                dispatchReceiver = irGet(builderLambda)
                putValueArgument(
                    index = 0,
                    valueArgument = irGet(temp),
                )
            }

            +irReturn(
                value = irCall(
                    callee = buildCall,
                ).apply {
                    dispatchReceiver = irGet(temp)
                },
            )
        }
    }
    //endregion

    private fun IrFunction.parentClassInstance(): IrGetValueImpl {
        return IrGetValueImpl(
            startOffset = -1,
            endOffset = -1,
            type = dispatchReceiverParameter!!.type,
            symbol = dispatchReceiverParameter!!.symbol,
        )
    }
    //endregion

    //region Constructor
    // Adapted from https://github.com/JetBrains/kotlin/blob/52a3ec9184fa44b2c8ce981f279cd66686dbe73b/plugins/plugin-sandbox/src/org/jetbrains/kotlin/plugin/sandbox/ir/AbstractTransformerForGenerator.kt#L89-L108
    override fun visitConstructor(declaration: IrConstructor) {
        val origin = declaration.origin
        if (origin !is GeneratedByPlugin || !interestedIn(origin.pluginKey) || declaration.body != null) {
            return
        }

        val type = declaration.returnType as? IrSimpleType ?: return
        val anyPrimaryConstructor = irBuiltIns.anyClass.owner.primaryConstructor ?: return
        val initializerSymbol = (declaration.parent as? IrClass)?.symbol ?: return
        declaration.body = DeclarationIrBuilder(
            generatorContext = context,
            symbol = declaration.symbol,
        ).irBlockBody {
            +irDelegatingConstructorCall(anyPrimaryConstructor)
            // TODO: Find factory helper for this?
            +IrInstanceInitializerCallImpl(
                startOffset = -1,
                endOffset = -1,
                classSymbol = initializerSymbol,
                type = type,
            )
        }
    }
    //endregion

    private fun interestedIn(
        key: GeneratedDeclarationKey?,
    ): Boolean {
        return key == BuilderFirDeclarationGenerationExtension.Key
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration, is IrFile, is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> Unit
        }
    }
}
