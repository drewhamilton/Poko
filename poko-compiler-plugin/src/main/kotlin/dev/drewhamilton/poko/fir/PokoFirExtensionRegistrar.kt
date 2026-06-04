package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.name.ClassId

internal class PokoFirExtensionRegistrar(
    private val pokoAnnotation: ClassId,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +PokoFirExtensionSessionComponent.getFactory(pokoAnnotation)
        +::PokoFirCheckersExtension
        +FirDeclarationGenerationExtension.Factory { session ->
            if (session is FirCliSession) {
                PokoFirDeclarationGenerationExtension(session)
            } else {
                NoOpExtension(session)
            }
        }
    }
}

private class NoOpExtension(
    session: FirSession,
) : FirDeclarationGenerationExtension(session)
