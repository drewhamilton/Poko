package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

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

    private val generatedBuilders = mutableListOf<ClassId>()

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(pokoAnnotationPredicate)
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> = when {
        classSymbol.classId in generatedBuilders -> setOf(SpecialNames.INIT)
        classSymbol.matchesPokoAnnotationPredicate() -> setOf(GeneratedBuilderName).also {
            generatedBuilders.add(classSymbol.classId.createNestedClassId(it.single()))
        }
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

    // FIXME: This isn't getting called
    override fun generateConstructors(
        context: MemberGenerationContext,
    ): List<FirConstructorSymbol> {
        return listOf(
            createConstructor(
                owner = context.owner,
                key = Key,
                isPrimary = true,
            ).symbol,
        )
    }

    private companion object {
        private val GeneratedBuilderName = Name.identifier("Builder")
    }

    internal object Key : GeneratedDeclarationKey() {
        override fun toString() = "Poko BuilderGeneratorExtension.Key"
    }
}
