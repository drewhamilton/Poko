package dev.drewhamilton.poko.fir

import dev.drewhamilton.poko.builder.BuilderFirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.name.ClassId

internal class PokoFirExtensionRegistrar(
    private val pokoAnnotation: ClassId,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +PokoFirExtensionSessionComponent.getFactory(pokoAnnotation)
        +::PokoFirCheckersExtension
        +::BuilderFirDeclarationGenerationExtension
    }
}
