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
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.primaryConstructor

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
        return null // TODO
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
