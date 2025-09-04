package dev.drewhamilton.poko.fir

import dev.drewhamilton.poko.PokoFunction
import dev.drewhamilton.poko.PokoFunction.Equals
import dev.drewhamilton.poko.PokoFunction.HashCode
import dev.drewhamilton.poko.PokoFunction.ToString
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
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
        classSymbol in pokoClasses -> PokoFunction.entries.map { it.functionName }.toSet()
        else -> emptySet()
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        val scope = context.declaredScope ?: return emptyList()

        val callableName = callableId.callableName
        val function = with(scope) {
            when (callableName) {
                Equals.functionName -> runIf(owner.canGenerateFunction(Equals)) {
                    createEqualsFunction(owner)
                }

                HashCode.functionName -> runIf(owner.canGenerateFunction(HashCode)) {
                    createHashCodeFunction(owner)
                }

                ToString.functionName -> runIf(owner.canGenerateFunction(ToString)) {
                    createToStringFunction(owner)
                }

                else -> null
            }
        }
        return function?.let { listOf(it.symbol) } ?: emptyList()
    }

    context(scope: FirClassDeclaredMemberScope)
    private fun FirClassSymbol<*>.canGenerateFunction(function: PokoFunction): Boolean {
        if (hasDeclaredFunction(function)) return false

        val superclassFunction = findNearestSuperclassFunction(function)

        return superclassFunction?.isOverridable ?: true
    }

    context(scope: FirClassDeclaredMemberScope)
    private fun hasDeclaredFunction(function: PokoFunction): Boolean {
        return declaredFunction(function) != null
    }

    /**
     * Finds the function symbol if the given [function] is declared in this scope.
     *
     * Used for the Poko class.
     */
    context(scope: FirClassDeclaredMemberScope)
    private fun declaredFunction(
        function: PokoFunction,
    ): FirNamedFunctionSymbol? {
        val matchingFunctions = mutableListOf<FirNamedFunctionSymbol>()
        scope.processFunctionsByName(function.functionName) { functionSymbol ->
            if (
                !functionSymbol.isExtension &&
                functionSymbol.valueParameterSymbols
                    .map { it.resolvedReturnType } == function.valueParameterTypes()
            ) {
                matchingFunctions.add(functionSymbol)
            }
        }
        return matchingFunctions
            .apply { check(size < 2) { "Found multiple identical functions" } }
            .singleOrNull()
    }

    /**
     * Recursively finds the [function] in this class's nearest superclass with the same signature.
     * Ignores super-interfaces.
     */
    private fun FirClassSymbol<*>.findNearestSuperclassFunction(
        function: PokoFunction,
    ): FirNamedFunctionSymbol? {
        val superclass = resolvedSuperTypes
            .mapNotNull { it.toClassSymbol(session) }
            .filter { it.classKind == ClassKind.CLASS }
            .apply { check(size < 2) { "Found multiple superclasses" } }
            .singleOrNull()
            ?: return null

        return superclass.declaredFunction(function)
            ?: superclass.findNearestSuperclassFunction(function)
    }

    /**
     * Finds the function symbol if the given [function] is declared in this class.
     *
     * Used for the Poko class's superclass(es).
     */
    private fun FirClassSymbol<*>.declaredFunction(
        function: PokoFunction,
    ): FirNamedFunctionSymbol? {
        val matchingFunctions = mutableListOf<FirNamedFunctionSymbol>()
        processAllDeclaredCallables(session) { callableSymbol ->
            if (
                callableSymbol is FirNamedFunctionSymbol &&
                !callableSymbol.isExtension &&
                callableSymbol.name == function.functionName &&
                callableSymbol.valueParameterSymbols
                    .map { it.resolvedReturnType } == function.valueParameterTypes()
            ) {
                matchingFunctions.add(callableSymbol)
            }
        }
        return matchingFunctions
            .apply { check(size < 2) { "Found multiple identical functions" } }
            .singleOrNull()
    }

    private fun PokoFunction.valueParameterTypes(): List<ConeKotlinType> = when (this) {
        Equals -> listOf(session.builtinTypes.nullableAnyType.coneType)
        HashCode -> emptyList()
        ToString -> emptyList()
    }

    private val FirNamedFunctionSymbol.isOverridable: Boolean
        get() = visibility != Visibilities.Private && !isFinal

    private fun createEqualsFunction(
        owner: FirClassSymbol<*>,
    ): FirSimpleFunction = createMemberFunction(
        owner = owner,
        key = PokoKey,
        name = Equals.functionName,
        returnType = session.builtinTypes.booleanType.coneType,
    ) {
        modality = Modality.OPEN
        status {
            isOperator = true
        }
        valueParameter(
            name = Name.identifier("other"),
            type = session.builtinTypes.nullableAnyType.coneType,
            key = PokoKey,
        )
    }

    private fun createHashCodeFunction(
        owner: FirClassSymbol<*>,
    ): FirSimpleFunction = createMemberFunction(
        owner = owner,
        key = PokoKey,
        name = HashCode.functionName,
        returnType = session.builtinTypes.intType.coneType,
    ) {
        modality = Modality.OPEN
    }

    private fun createToStringFunction(
        owner: FirClassSymbol<*>,
    ): FirSimpleFunction = createMemberFunction(
        owner = owner,
        key = PokoKey,
        name = ToString.functionName,
        returnType = session.builtinTypes.stringType.coneType,
    ) {
        modality = Modality.OPEN
    }
}
