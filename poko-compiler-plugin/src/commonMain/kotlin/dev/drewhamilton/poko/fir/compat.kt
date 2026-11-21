package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

internal fun FirCallableSymbol<*>.isExtensionCompat(): Boolean {
    return try {
        isExtension
    } catch (_: NoSuchMethodError) {
        // 2.4.0-dev (IJ 2026.1):
        javaClass.classLoader
            .loadClass("org.jetbrains.kotlin.fir.declarations.utils.FirDeclarationUtilKt")
            .methods
            .single { it.name == "isExtension" &&
                it.parameters.size == 1 &&
                it.parameters[0].type == FirCallableSymbol::class.java
            }
            .invoke(
                null, // Static invocation
                this, // Extension receiver
            ) as Boolean
    }
}
