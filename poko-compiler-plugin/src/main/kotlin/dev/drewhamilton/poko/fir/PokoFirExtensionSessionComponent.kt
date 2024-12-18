package dev.drewhamilton.poko.fir

import dev.drewhamilton.poko.PokoAnnotationNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.name.ClassId

internal class PokoFirExtensionSessionComponent(
    session: FirSession,
    internal val pokoAnnotation: ClassId,
) : FirExtensionSessionComponent(session) {

    internal val pokoSkipAnnotation: ClassId =
        pokoAnnotation.createNestedClassId(PokoAnnotationNames.Skip)

    internal companion object {
        internal fun getFactory(pokoAnnotation: ClassId): Factory {
            return Factory { session ->
                PokoFirExtensionSessionComponent(session, pokoAnnotation)
            }
        }
    }
}

internal val FirSession.pokoFirExtensionSessionComponent: PokoFirExtensionSessionComponent by FirSession.sessionComponentAccessor()
