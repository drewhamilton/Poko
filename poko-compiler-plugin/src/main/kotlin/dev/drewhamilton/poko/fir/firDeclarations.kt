package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty

/**
 * Filters the list of declarations to a list of properties that are also parameters, necessarily
 * implying that those properties are declared in the primary constructor.
 */
internal fun List<FirDeclaration>.constructorProperties(): List<FirProperty> {
    return this
        .filterIsInstance<FirProperty>()
        .filter {
            it.source?.kind is KtFakeSourceElementKind.PropertyFromParameter
        }
}
