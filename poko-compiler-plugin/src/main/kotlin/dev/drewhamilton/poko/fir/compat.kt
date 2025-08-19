package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

internal fun FirCallableSymbol<*>.isExtensionCompat(): Boolean {
    return try {
        isExtension
    } catch (_: NoSuchMethodError) {
        // 2.2.0:
        javaClass.classLoader
            .loadClass("org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbolKt")
            .methods
            .single {
                it.name == "isExtension" &&
                    it.parameters.size == 1 &&
                    it.parameters.single().type == FirCallableSymbol::class.java
            }
            .invoke(
                null, // Static invocation
                this, // Extension receiver
            ) as Boolean
    }
}