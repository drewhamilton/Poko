/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package dev.drewhamilton.poko.ir

import dev.drewhamilton.poko.fir.PokoBuilderGeneratorExtension
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class TransformerForBuilderGenerator(
    context: IrPluginContext,
) : AbstractTransformerForGenerator(context, visitBodies = false) {

    override fun interestedIn(
        key: GeneratedDeclarationKey?,
    ): Boolean {
        return key == PokoBuilderGeneratorExtension.Key
    }

    override fun generateBodyForFunction(
        function: IrSimpleFunction,
        key: GeneratedDeclarationKey?,
    ): IrBody? {
        return if (
            function.extensionReceiverParameter == null &&
            function.typeParameters.isEmpty() &&
            function.valueParameters.size == 1 &&
            function.returnType.classOrNull?.owner == function.parent
        ) {
            generateBuilderSetterFunctionBody(function)
        } else {
            // TODO
            null
        }
    }

    private fun generateBuilderSetterFunctionBody(
        function: IrFunction,
    ): IrBody {
        val param = function.valueParameters.single()
        val parentClassInstance = IrGetValueImpl(
            startOffset = -1,
            endOffset = -1,
            type = function.dispatchReceiverParameter!!.type,
            symbol = function.dispatchReceiverParameter!!.symbol,
        )
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
