package dev.drewhamilton.poko.fir

import dev.drewhamilton.poko.FirIdeMode
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.name.ClassId

internal class PokoFirExtensionRegistrar(
    private val pokoAnnotation: ClassId,
    private val firIdeMode: FirIdeMode,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +PokoFirExtensionSessionComponent.getFactory(pokoAnnotation)
        +FirAdditionalCheckersExtension.Factory { session ->
            if (session.isCli() || firIdeMode != FirIdeMode.NONE) {
                PokoFirCheckersExtension(session)
            } else {
                NoOpCheckersExtension(session)
            }
        }
        +FirDeclarationGenerationExtension.Factory { session ->
            if (session.isCli() || firIdeMode == FirIdeMode.ALL) {
                PokoFirDeclarationGenerationExtension(session)
            } else {
                NoOpDeclarationGenerationExtension(session)
            }
        }
    }
}

private class NoOpCheckersExtension(
    session: FirSession,
) : FirAdditionalCheckersExtension(session)

private class NoOpDeclarationGenerationExtension(
    session: FirSession,
) : FirDeclarationGenerationExtension(session)

@Suppress("NOTHING_TO_INLINE")
private inline fun FirSession.isCli(): Boolean {
    return this is FirCliSession
}
