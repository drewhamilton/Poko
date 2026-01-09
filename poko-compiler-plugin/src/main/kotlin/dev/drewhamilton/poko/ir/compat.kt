package dev.drewhamilton.poko.ir

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

internal val IrDeclarationOrigin.Companion.DEFINED_COMPAT: IrDeclarationOrigin
    get() = try {
        DEFINED
    } catch (_: NoSuchMethodError) {
        // 2.3.0:
        this::class.java.classLoader
            .loadClass($$"org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin$Companion")
            .methods
            .single { it.name == "getDEFINED" && it.parameters.isEmpty() }
            .invoke(
                this, // Dispatch receiver
            ) as IrDeclarationOrigin
    }
