package dev.drewhamilton.poko.fir

import dev.drewhamilton.poko.PokoAnnotationNames
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.FirAnnotationArgumentMappingBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirAnnotationBuilder
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.FirResolvedTypeRefBuilder
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class PokoBuilderGeneratorExtension(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {

    private val pokoBuilderAnnotation by lazy {
        session.pokoFirExtensionSessionComponent.pokoBuilderAnnotation
    }

    private val pokoBuilderAnnotationPredicate by lazy {
        LookupPredicate.create {
            annotated(pokoBuilderAnnotation.asSingleFqName())
        }
    }

    /**
     * Pairs of <Poko.Builder ClassId, outer Poko class Symbol>.
     */
    private val pokoBuilderClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(pokoBuilderAnnotationPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
            .associateBy { it.classId.createNestedClassId(PokoAnnotationNames.Builder) }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(pokoBuilderAnnotationPredicate)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> = when {
        classSymbol.classId in pokoBuilderClasses.keys -> {
            setOf(SpecialNames.INIT, BUILD_FUNCTION_NAME) +
                classSymbol.outerClassConstructorProperties().map { it.name }
        }
        else -> emptySet()
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> = when {
        classSymbol in pokoBuilderClasses.values -> setOf(PokoAnnotationNames.Builder)
        else -> emptySet()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        return when (name) {
            PokoAnnotationNames.Builder -> {
                if (owner !in pokoBuilderClasses.values) return null
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
        // No special properties:
        if (callableId.callableName.isSpecial) return emptyList()

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
            ).apply {
                applyJvmSyntheticSetterAnnotation()
            }.symbol,
        )
    }

    private fun FirProperty.applyJvmSyntheticSetterAnnotation() {
        setter!!.replaceAnnotations(
            listOf(jvmSyntheticAnnotation(AnnotationUseSiteTarget.PROPERTY_SETTER)),
        )
    }

    // Copied from debugging session with real `@set:JvmSynthetic` annotation:
    @Suppress("SameParameterValue")
    private fun jvmSyntheticAnnotation(
        useSiteTarget: AnnotationUseSiteTarget,
    ): FirAnnotation = FirAnnotationBuilder().apply {
        this.useSiteTarget = useSiteTarget

        annotationTypeRef = FirResolvedTypeRefBuilder().apply {
            coneType = ConeClassLikeTypeImpl(
                lookupTag = ConeClassLikeLookupTagImpl(
                    classId = ClassId.fromString("kotlin/jvm/JvmSynthetic"),
                ),
                typeArguments = emptyArray(),
                isMarkedNullable = false,
            )
        }.build()

        argumentMapping = FirAnnotationArgumentMappingBuilder().build()
    }.build()

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()

        val callableName = callableId.callableName
        val function = if (callableName.isSpecial) {
            when (callableName) {
                BUILD_FUNCTION_NAME -> createBuildFunction(owner, callableName)
                else -> throw IllegalArgumentException("Unknown function name: $callableName")
            }
        } else {
            createSetterFunction(owner, callableName)
        }
        return listOf(function.symbol)
    }

    private fun FirExtension.createBuildFunction(
        owner: FirClassSymbol<*>,
        callableName: Name,
    ): FirSimpleFunction = createMemberFunction(
        owner = owner,
        key = Key,
        name = callableName.unSpecial(),
        returnType = pokoBuilderClasses.getValue(owner.classId).defaultType(),
    )

    private fun Name.unSpecial(): Name {
        require(isSpecial) { "Can't unspecial a name that is already not special" }
        return Name.identifier(asStringStripSpecialMarkers())
    }

    private fun FirExtension.createSetterFunction(
        owner: FirClassSymbol<*>,
        callableName: Name,
    ): FirSimpleFunction = createMemberFunction(
        owner = owner,
        key = Key,
        // TODO: Copy out this JvmAbi implementation for safety?
        name = Name.identifier(JvmAbi.setterName(callableName.identifier)),
        returnType = owner.defaultType(),
        config = {
            valueParameter(
                name = callableName,
                type = owner.outerClassConstructorProperties().single {
                    it.name == callableName
                }.returnTypeRef.coneType,
            )
        }
    )

    // TODO: Try to use map instead
    private fun FirClassSymbol<*>.outerClassConstructorProperties(): List<FirProperty> {
        // TODO: Is this opt-in dangerous?
        @OptIn(SymbolInternals::class)
        val containingClass = getContainingDeclaration(session)!!.fir as FirClass
        return containingClass.declarations.constructorProperties()
    }

    internal object Key : GeneratedDeclarationKey() {
        override fun toString() = "PokoBuilderGeneratorExtension.Key"
    }

    private companion object {
        // TODO: Is it dangerous to abuse `special` like this?
        private val BUILD_FUNCTION_NAME = Name.special("<build>")
    }
}
