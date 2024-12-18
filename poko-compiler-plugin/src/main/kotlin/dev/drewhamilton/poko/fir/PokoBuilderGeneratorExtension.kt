package dev.drewhamilton.poko.fir

import dev.drewhamilton.poko.PokoAnnotationNames
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class PokoBuilderGeneratorExtension(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {

    private val pokoBuilderAnnotation by lazy {
        session.pokoFirExtensionSessionComponent.pokoBuilderAnnotation
    }

    private val pokoAnnotationPredicate by lazy {
        LookupPredicate.create {
            annotated(pokoBuilderAnnotation.asSingleFqName())
        }
    }

    private val pokoClassIds by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(pokoAnnotationPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
    }

    private val builderClassIds by lazy {
        pokoClassIds.map { it.classId.createNestedClassId(PokoAnnotationNames.Builder) }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(pokoAnnotationPredicate)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> = when {
        classSymbol.classId in builderClassIds -> {
            setOf(SpecialNames.INIT) + classSymbol.outerClassConstructorProperties().map { it.name }
        }
        else -> emptySet()
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> = when {
        classSymbol in pokoClassIds -> setOf(PokoAnnotationNames.Builder)
        else -> emptySet()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        return when (name) {
            PokoAnnotationNames.Builder -> {
                if (owner !in pokoClassIds) return null
                createNestedClass(
                    owner = owner,
                    name = name,
                    key = Key,
                ).symbol
            }
            else -> null
        }
    }

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

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        val returnType = owner.outerClassConstructorProperties().single {
            it.name == callableId.callableName
        }
        return listOf(
            createMemberProperty(
                owner = owner,
                key = Key,
                name = callableId.callableName,
                returnType = returnType.returnTypeRef.coneType.withNullability(
                    nullable = true,
                    typeContext = session.typeContext,
                ),
                isVal = false,
                hasBackingField = false,
            ).symbol,
        )
    }

    private fun FirClassSymbol<*>.outerClassConstructorProperties(): List<FirProperty> {
        // TODO: Is this opt-in dangerous?
        @OptIn(SymbolInternals::class)
        val containingClass = getContainingDeclaration(session)!!.fir as FirClass
        return containingClass.declarations.constructorProperties()
    }

    internal object Key : GeneratedDeclarationKey() {
        override fun toString() = "PokoBuilderGeneratorExtension.Key"
    }
}
