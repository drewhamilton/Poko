package dev.drewhamilton.poko.fir

import dev.drewhamilton.poko.BuildConfig.DEFAULT_POKO_ANNOTATION
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.isArrayOrPrimitiveArray
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty

internal class PokoFirCheckersExtension(
    session: FirSession,
) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers =
        object : DeclarationCheckers() {
            override val regularClassCheckers: Set<FirRegularClassChecker> =
                setOf(PokoFirRegularClassChecker)
        }

    private object PokoFirRegularClassChecker : FirRegularClassChecker(
        mppKind = MppCheckerKind.Common,
    ) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirRegularClass) {
            if (!declaration.hasAnyPokoClassAnnotation(context)) return

            val errorFactory = when {
                declaration.classKind != ClassKind.CLASS -> Diagnostics.PokoOnNonClass
                declaration.isData -> Diagnostics.PokoOnDataClass
                declaration.hasModifier(KtTokens.VALUE_KEYWORD) -> Diagnostics.PokoOnValueClass
                declaration.isInner -> Diagnostics.PokoOnInnerClass
                declaration.primaryConstructorIfAny(context.session) == null ->
                    Diagnostics.PrimaryConstructorRequired
                else -> null
            }
            if (errorFactory != null) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = errorFactory,
                )
            }

            val sessionComponent = context.session.pokoFirExtensionSessionComponent
            val constructorProperties = mutableListOf<FirPropertySymbol>()
            declaration.processAllDeclarations(context.session) { declarationSymbol ->
                if (
                    declarationSymbol is FirPropertySymbol &&
                    declarationSymbol.source?.kind is KtFakeSourceElementKind.PropertyFromParameter
                ) {
                    constructorProperties.add(declarationSymbol)
                }
            }

            val skipAnnotation = sessionComponent.pokoSkipAnnotation
            val filteredConstructorProperties = constructorProperties
                .filter {
                    val hasSkipAnnotation = it.hasAnnotation(skipAnnotation, context.session)
                    if (hasSkipAnnotation && !skipAnnotation.isNestedInDefaultPokoAnnotation()) {
                        // Pseudo-opt-in warning for custom annotation consumers:
                        reporter.reportOn(
                            source = it.source,
                            factory = Diagnostics.SkippedPropertyWithCustomAnnotation,
                        )
                    }
                    !hasSkipAnnotation
                }
                .onEach { propertySymbol ->
                    val hasReadArrayContentAnnotation = propertySymbol.hasAnnotation(
                        classId = sessionComponent.pokoReadArrayContentAnnotation,
                        session = context.session,
                    )
                    val propertyType = propertySymbol.resolvedReturnType
                    if (
                        hasReadArrayContentAnnotation &&
                        !propertyType.isArrayOrPrimitiveArray &&
                        !propertyType.mayBeRuntimeArray()
                    ) {
                        reporter.reportOn(
                            source = propertySymbol.source,
                            factory = Diagnostics.ReadArrayContentOnNonArrayProperty,
                        )
                    }
                }
            if (filteredConstructorProperties.isEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = Diagnostics.PrimaryConstructorPropertiesRequired,
                )
            }
        }

        private fun FirRegularClass.hasAnyPokoClassAnnotation(
            context: CheckerContext,
        ): Boolean {
            val sessionComponent = context.session.pokoFirExtensionSessionComponent
            return hasAnnotation(sessionComponent.pokoAnnotation) ||
                hasAnnotation(sessionComponent.pokoEqualsAndHashCodeAnnotation) ||
                hasAnnotation(sessionComponent.pokoToStringAnnotation)
        }

        private fun FirDeclaration.hasAnnotation(
            annotation: ClassId,
        ): Boolean {
            return annotations.any { firAnnotation ->
                firAnnotation.classId() == annotation
            }
        }

        private fun FirAnnotation.classId(): ClassId? {
            return annotationTypeRef.coneTypeOrNull?.classId
        }

        private fun ClassId.isNestedInDefaultPokoAnnotation(): Boolean {
            val outerFqName = outerClassId?.asFqNameString()
            return outerFqName == DEFAULT_POKO_ANNOTATION ||
                // Multiplatform FqName has "." instead of "/" for package:
                outerFqName?.replace(".", "/") == DEFAULT_POKO_ANNOTATION
        }

        /**
         * Returns true if the property represents a type that may be an array at runtime (e.g.
         * [Any] or a generic type).
         */
        context(context: CheckerContext)
        private fun ConeKotlinType.mayBeRuntimeArray(): Boolean {
            val builtinTypes = context.session.builtinTypes
            return this == builtinTypes.anyType.coneType ||
                this == builtinTypes.nullableAnyType.coneType ||
                (this is ConeTypeParameterType && hasArrayOrPrimitiveArrayUpperBound())
        }

        context(context: CheckerContext)
        private fun ConeTypeParameterType.hasArrayOrPrimitiveArrayUpperBound(): Boolean {
            val builtinTypes = context.session.builtinTypes
            lookupTag.typeParameterSymbol.resolvedBounds.forEach { resolvedBound ->
                val resolvedBoundConeType = resolvedBound.coneType
                // Note: A generic type cannot have an array as an upper bound, else that would also be
                // checked here.
                val foundUpperBoundMatch = resolvedBoundConeType == builtinTypes.anyType.coneType ||
                    resolvedBoundConeType == builtinTypes.nullableAnyType.coneType ||
                    (resolvedBoundConeType is ConeTypeParameterType &&
                        resolvedBoundConeType.hasArrayOrPrimitiveArrayUpperBound())

                if (foundUpperBoundMatch) {
                    return true
                }
            }

            return false
        }
    }

    private object Diagnostics : KtDiagnosticsContainer() {
        val PokoOnNonClass by error0<KtClass>(
            positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
        )

        val PokoOnDataClass by error0<KtClass>(
            positioningStrategy = SourceElementPositioningStrategies.DATA_MODIFIER,
        )

        val PokoOnValueClass by error0<KtClass>(
            positioningStrategy = SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER,
        )

        val PokoOnInnerClass by error0<KtClass>(
            positioningStrategy = SourceElementPositioningStrategies.INNER_MODIFIER,
        )

        val PrimaryConstructorRequired by error0<KtClass>(
            positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
        )

        val PrimaryConstructorPropertiesRequired by error0<KtClass>(
            positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
        )

        val SkippedPropertyWithCustomAnnotation by warning0<KtProperty>(
            positioningStrategy = SourceElementPositioningStrategies.ANNOTATION_USE_SITE,
        )

        val ReadArrayContentOnNonArrayProperty by error0<KtProperty>(
            positioningStrategy = SourceElementPositioningStrategies.ANNOTATION_USE_SITE,
        )

        override fun getRendererFactory(): BaseDiagnosticRendererFactory = DiagnosticRendererFactory
    }

    private object DiagnosticRendererFactory : BaseDiagnosticRendererFactory() {
        override val MAP by KtDiagnosticFactoryToRendererMap("Poko") {
            it.put(
                factory = Diagnostics.PokoOnNonClass,
                message = "Poko can only be applied to a class",
            )
            it.put(
                factory = Diagnostics.PokoOnDataClass,
                message = "Poko cannot be applied to a data class",
            )
            it.put(
                factory = Diagnostics.PokoOnValueClass,
                message = "Poko cannot be applied to a value class",
            )
            it.put(
                factory = Diagnostics.PokoOnInnerClass,
                message = "Poko cannot be applied to an inner class"
            )
            it.put(
                factory = Diagnostics.PrimaryConstructorRequired,
                message = "Poko class must have a primary constructor"
            )
            it.put(
                factory = Diagnostics.PrimaryConstructorPropertiesRequired,
                message = "Poko class primary constructor must have at least one not-skipped property",
            )
            it.put(
                factory = Diagnostics.SkippedPropertyWithCustomAnnotation,
                message = "The @Skip annotation is experimental and its behavior may change; use with caution",
            )
            it.put(
                factory = Diagnostics.ReadArrayContentOnNonArrayProperty,
                message = "@ReadArrayContent is only supported on properties with array type or `Any` type"
            )
        }
    }
}
