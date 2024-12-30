package dev.drewhamilton.poko.builder

import dev.drewhamilton.poko.fir.constructorProperties
import dev.drewhamilton.poko.fir.pokoFirExtensionSessionComponent
import dev.drewhamilton.poko.unSpecial
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirAnnotationArgumentMappingBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirAnnotationBuilder
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
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

internal class BuilderFirDeclarationGenerationExtension(
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
     * Pairs of <Poko.Builder ClassId, outer class Symbol>.
     */
    private val pokoBuilderClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(pokoBuilderAnnotationPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
            .associateBy { it.classId.createNestedClassId(BuilderClassName) }
    }

    private val factoryFunctionIds by lazy {
        pokoBuilderClasses.values
            .map {
                CallableId(
                    packageName = it.packageFqName(),
                    callableName = it.name,
                )
            }
            .toSet()
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(pokoBuilderAnnotationPredicate)
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun getTopLevelCallableIds(): Set<CallableId> {
        return factoryFunctionIds
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> = when {
        classSymbol.classId in pokoBuilderClasses.keys -> {
            setOf(SpecialNames.INIT, BuildFunctionSpecialName) +
                pokoBuilderClasses.getValue(classSymbol.classId).constructorProperties().map {
                    it.name
                }
        }
        else -> emptySet()
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> = when {
        classSymbol in pokoBuilderClasses.values -> setOf(BuilderClassName)
        else -> emptySet()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        return when (name) {
            BuilderClassName -> {
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
        // No top-level properties:
        if (callableId.className == null) return emptyList()
        // No special properties:
        if (callableId.callableName.isSpecial) return emptyList()

        val owner = context?.owner ?: return emptyList()
        val originalProperty = pokoBuilderClasses.getValue(owner.classId)
            .constructorProperties()
            .single { it.name == callableId.callableName }
        return listOf(
            createMemberProperty(
                owner = owner,
                key = Key,
                name = callableId.callableName,
                returnType = originalProperty.returnTypeRef.coneType.withNullability(
                    nullable = true,
                    typeContext = session.typeContext,
                ),
                isVal = false,
            ).apply {
                setter!!.replaceAnnotations(
                    listOf(jvmSyntheticAnnotation(AnnotationUseSiteTarget.PROPERTY_SETTER)),
                )
                applyOriginalDefaultExpression(originalProperty)
            }.symbol,
        )
    }

    private fun FirProperty.applyOriginalDefaultExpression(
        originalProperty: FirProperty,
    ) {
        val originalDefaultExpression = originalProperty
            .correspondingValueParameterFromPrimaryConstructor!!
            .resolvedDefaultValue

        val initializer = when (originalDefaultExpression) {
            null -> return
            is FirLiteralExpression -> originalDefaultExpression
            // TODO: Handle other types of default expression and/or log this
            else -> return
        }
        replaceInitializer(initializer)
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
        val owner = context?.owner

        val callableName = callableId.callableName
        val function = when {
            context == null && callableId in factoryFunctionIds -> {
                createFactoryFunction(callableId)
            }

            owner == null -> return emptyList()

            callableName.isSpecial -> when (callableName) {
                BuildFunctionSpecialName -> createBuildFunction(owner, callableName)
                else -> throw IllegalArgumentException("Unknown function name: $callableName")
            }

            else -> createSetterFunction(owner, callableName)
        }
        return listOf(function.symbol)
    }

    @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
    private fun createFactoryFunction(
        callableId: CallableId,
    ): FirSimpleFunction = createTopLevelFunction(
        key = Key,
        callableId = callableId,
        returnType = pokoBuilderClasses.values
            .single { it.name == callableId.callableName }
            .defaultType(),
        config = {
            valueParameter(
                name = Name.identifier("initializer"),
                type = ConeClassLikeTypeImpl(
                    lookupTag = ConeClassLikeLookupTagImpl(
                        classId = ClassId.fromString("kotlin/Function1"),
                    ),
                    typeArguments = arrayOf(
                        ConeClassLikeTypeImpl(
                            lookupTag = pokoBuilderClasses.entries
                                .single { it.value.name == callableId.callableName }
                                .key
                                .createConeType(session)
                                .lookupTag,
                            typeArguments = emptyArray(),
                            isMarkedNullable = false,
                        ),
                        ConeClassLikeTypeImpl(
                            lookupTag = ConeClassLikeLookupTagImpl(
                                classId = ClassId.fromString("kotlin/Unit"),
                            ),
                            typeArguments = emptyArray(),
                            isMarkedNullable = false,
                        ),
                    ),
                    isMarkedNullable = false,
                    attributes = ConeAttributes.WithExtensionFunctionType,
                ),
            )
        },
    ).apply {
        replaceAnnotations(
            listOf(jvmSyntheticAnnotation(AnnotationUseSiteTarget.RECEIVER)),
        )
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
                type = pokoBuilderClasses.getValue(owner.classId).constructorProperties().single {
                    it.name == callableName
                }.returnTypeRef.coneType,
            )
        }
    )

    // TODO: Is this opt-in dangerous?
    @OptIn(SymbolInternals::class)
    private fun FirClassSymbol<*>.constructorProperties(): List<FirProperty> {
        return fir.declarations.constructorProperties()
    }

    internal object Key : GeneratedDeclarationKey() {
        override fun toString() = "Poko BuilderFirDeclarationGenerationExtension.Key"
    }
}
