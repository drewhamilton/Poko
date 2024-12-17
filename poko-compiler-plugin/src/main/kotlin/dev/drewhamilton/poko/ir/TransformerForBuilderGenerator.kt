/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package dev.drewhamilton.poko.ir

import dev.drewhamilton.poko.fir.BuilderGeneratorExtension
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody

internal class TransformerForBuilderGenerator(
    context: IrPluginContext,
) : AbstractTransformerForGenerator(context, visitBodies = false) {
    override fun interestedIn(
        key: GeneratedDeclarationKey?,
    ): Boolean {
        return key == BuilderGeneratorExtension.Key
    }

    override fun generateBodyForFunction(
        function: IrSimpleFunction,
        key: GeneratedDeclarationKey?,
    ): IrBody? {
        return null // TODO
    }

    override fun generateBodyForConstructor(
        constructor: IrConstructor,
        key: GeneratedDeclarationKey?,
    ): IrBody? {
        return constructor.body
    }
}
