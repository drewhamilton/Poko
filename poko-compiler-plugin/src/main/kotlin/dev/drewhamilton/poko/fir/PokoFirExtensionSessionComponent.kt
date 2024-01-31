package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId

internal class PokoFirExtensionSessionComponent(
    session: FirSession,
    private val pokoAnnotation: ClassId,
) : FirExtensionSessionComponent(session) {
    fun pokoAnnotation(declaration: FirDeclaration): FirAnnotation? {
        return declaration.annotations.firstOrNull { firAnnotation ->
            firAnnotation.classId() == pokoAnnotation
        }
    }

    private fun FirAnnotation.classId(): ClassId? =
        annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId

    internal companion object {
        internal fun getFactory(pokoAnnotation: ClassId): Factory {
            return Factory { session ->
                PokoFirExtensionSessionComponent(session, pokoAnnotation)
            }
        }
    }
}

internal val FirSession.pokoFirExtensionSessionComponent: PokoFirExtensionSessionComponent by FirSession.sessionComponentAccessor()
