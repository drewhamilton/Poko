package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.name.ClassId

internal class PokoFirExtensionRegistrar(
    private val pokoAnnotation: ClassId,
    private val declarationGeneration: Boolean,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +PokoFirExtensionSessionComponent.getFactory(pokoAnnotation)
        +::PokoFirCheckersExtension
        if (declarationGeneration) {
            +::PokoFirDeclarationGenerationExtension
        }
    }
}
