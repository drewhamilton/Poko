package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension

internal fun FirCallableSymbol<*>.isExtensionCompat(): Boolean {
    return try {
        isExtension
    } catch (noSuchMethodError: NoSuchMethodError) {
        // 2.2.20:
        javaClass.classLoader
            .loadClass("org.jetbrains.kotlin.fir.declarations.utils.FirDeclarationUtilKt")
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