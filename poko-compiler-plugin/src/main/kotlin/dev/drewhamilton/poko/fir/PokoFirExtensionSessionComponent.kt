package dev.drewhamilton.poko.fir

import dev.drewhamilton.poko.BuildConfig.SKIP_ANNOTATION_SHORT_NAME
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class PokoFirExtensionSessionComponent(
    session: FirSession,
    private val pokoAnnotation: ClassId,
) : FirExtensionSessionComponent(session) {
    fun pokoAnnotation(declaration: FirDeclaration): FirAnnotation? {
        return declaration.annotations.firstOrNull { firAnnotation ->
            firAnnotation.classId() == pokoAnnotation
        }
    }

    fun pokoSkipAnnotation(declaration: FirDeclaration): FirAnnotation? {
        val skipAnnotation = pokoAnnotation.createNestedClassId(
            name = Name.identifier(SKIP_ANNOTATION_SHORT_NAME),
        )
        return declaration.annotations.firstOrNull { firAnnotation ->
            firAnnotation.classId() == skipAnnotation
        }
    }

    private fun FirAnnotation.classId(): ClassId? {
        return annotationTypeRef.coneTypeOrNull?.classId
    }

    internal companion object {
        internal fun getFactory(pokoAnnotation: ClassId): Factory {
            return Factory { session ->
                PokoFirExtensionSessionComponent(session, pokoAnnotation)
            }
        }
    }
}

internal val FirSession.pokoFirExtensionSessionComponent: PokoFirExtensionSessionComponent by FirSession.sessionComponentAccessor()
