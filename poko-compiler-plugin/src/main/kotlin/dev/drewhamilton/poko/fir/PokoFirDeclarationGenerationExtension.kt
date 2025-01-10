package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal class PokoFirDeclarationGenerationExtension(
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

    /**
     * Pairs of <Poko.Builder ClassId, outer class Symbol>.
     */
    private val pokoClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(pokoAnnotationPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(pokoAnnotationPredicate)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> = when {
        classSymbol in pokoClasses -> setOf(EqualsName, HashCodeName, ToStringName)
        else -> emptySet()
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()

        val callableName = callableId.callableName
        val function = when (callableName) {
            EqualsName -> runIf(!owner.hasDeclaredEqualsFunction()) {
                createEqualsFunction(owner)
            }

            HashCodeName -> runIf(!owner.hasDeclaredHashCodeFunction()) {
                createHashCodeFunction(owner)
            }

            ToStringName -> runIf(!owner.hasDeclaredToStringFunction()) {
                createToStringFunction(owner)
            }

            else -> null
        }
        return function?.let { listOf(it.symbol) } ?: emptyList()
    }

    //region equals
    private fun FirClassSymbol<*>.hasDeclaredEqualsFunction(): Boolean {
        return declarationSymbols
            .filterIsInstance<FirNamedFunctionSymbol>()
            .any {
                !it.isExtension &&
                    it.name == EqualsName &&
                    it.valueParameterSymbols.size == 1 &&
                    it.valueParameterSymbols
                        .single()
                        .resolvedReturnType == session.builtinTypes.nullableAnyType.coneType
            }
    }

    private fun createEqualsFunction(
        owner: FirClassSymbol<*>,
    ): FirSimpleFunction = createMemberFunction(
        owner = owner,
        key = PokoKey,
        name = EqualsName,
        returnType = session.builtinTypes.booleanType.coneType,
    ) {
        valueParameter(
            name = Name.identifier("other"),
            type = session.builtinTypes.nullableAnyType.coneType,
            key = PokoKey,
        )
    }
    //endregion

    //region hashCode
    private fun FirClassSymbol<*>.hasDeclaredHashCodeFunction(): Boolean {
        return declarationSymbols
            .filterIsInstance<FirNamedFunctionSymbol>()
            .any {
                !it.isExtension &&
                    it.name == HashCodeName &&
                    it.valueParameterSymbols.isEmpty()
            }
    }

    private fun createHashCodeFunction(
        owner: FirClassSymbol<*>,
    ): FirSimpleFunction = createMemberFunction(
        owner = owner,
        key = PokoKey,
        name = HashCodeName,
        returnType = session.builtinTypes.intType.coneType,
    )
    //endregion

    //region toString
    private fun FirClassSymbol<*>.hasDeclaredToStringFunction(): Boolean {
        return declarationSymbols
            .filterIsInstance<FirNamedFunctionSymbol>()
            .any {
                !it.isExtension &&
                    it.name == ToStringName &&
                    it.valueParameterSymbols.isEmpty()
            }
    }

    private fun createToStringFunction(
        owner: FirClassSymbol<*>,
    ): FirSimpleFunction = createMemberFunction(
        owner = owner,
        key = PokoKey,
        name = ToStringName,
        returnType = session.builtinTypes.stringType.coneType,
    )
    //endregion

    private companion object {
        private val EqualsName = OperatorNameConventions.EQUALS
        private val HashCodeName = OperatorNameConventions.HASH_CODE
        private val ToStringName = OperatorNameConventions.TO_STRING
    }
}
