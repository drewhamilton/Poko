package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.Name

internal class BuilderGeneratorExtension(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {

    private val pokoAnnotation by lazy {
        session.pokoFirExtensionSessionComponent.pokoAnnotation
    }

    private val pokoAnnotationPredicate by lazy {
        LookupPredicate.create {
            annotated(pokoAnnotation.asSingleFqName())
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(pokoAnnotationPredicate)
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> = when {
        classSymbol.matchesPokoAnnotationPredicate() -> setOf(GeneratedBuilderName)
        else -> emptySet()
    }

    private fun FirClassSymbol<*>.matchesPokoAnnotationPredicate(): Boolean {
        return session.predicateBasedProvider.matches(pokoAnnotationPredicate, this)
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        return when (name) {
            GeneratedBuilderName -> {
                if (!owner.matchesPokoAnnotationPredicate()) return null
                createNestedClass(
                    owner = owner,
                    name = name,
                    key = Key,
                ).symbol
            }
            else -> null
        }
    }

    private companion object {
        private val GeneratedBuilderName = Name.identifier("Builder")
    }

    internal object Key : GeneratedDeclarationKey() {
        override fun toString() = "Poko BuilderGeneratorExtension.Key"
    }
}
