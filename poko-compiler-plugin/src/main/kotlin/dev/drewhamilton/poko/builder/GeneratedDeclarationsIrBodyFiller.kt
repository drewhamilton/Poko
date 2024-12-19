/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package dev.drewhamilton.poko.builder

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

// Adapted from https://github.com/JetBrains/kotlin/blob/52a3ec9184fa44b2c8ce981f279cd66686dbe73b/plugins/plugin-sandbox/src/org/jetbrains/kotlin/plugin/sandbox/ir/GeneratedDeclarationsIrBodyFiller.kt
internal class GeneratedDeclarationsIrBodyFiller : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val transformers = listOf(
            TransformerForBuilderGenerator(pluginContext),
        )

        for (transformer in transformers) {
            moduleFragment.acceptChildrenVoid(transformer)
        }
    }
}
