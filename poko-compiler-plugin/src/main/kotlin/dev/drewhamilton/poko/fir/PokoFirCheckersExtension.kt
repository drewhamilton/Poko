package dev.drewhamilton.poko.fir

import dev.drewhamilton.poko.BuildConfig.DEFAULT_POKO_ANNOTATION
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.AbstractSourceElementPositioningStrategy
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0DelegateProvider
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.reportOn
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
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId

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
            val sessionComponent = context.session.pokoFirExtensionSessionComponent
            if (!declaration.hasAnnotation(sessionComponent.pokoAnnotation)) return

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
            if (filteredConstructorProperties.isEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = Diagnostics.PrimaryConstructorPropertiesRequired,
                )
            }
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
    }

    private object Diagnostics : BaseDiagnosticRendererFactory() {

        /**
         * The compiler and the IDE use a different version of this class, so use reflection to find the available
         * version.
         */
        // Adapted from https://github.com/TadeasKriz/K2PluginBase/blob/main/kotlin-plugin/src/main/kotlin/com/tadeaskriz/example/ExamplePluginErrors.kt#L8
        private val psiElementClass by lazy {
            try {
                Class.forName("org.jetbrains.kotlin.com.intellij.psi.PsiElement")
            } catch (_: ClassNotFoundException) {
                Class.forName("com.intellij.psi.PsiElement")
            }.kotlin
        }

        val PokoOnNonClass by error0(
            positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
        )

        val PokoOnDataClass by error0(
            positioningStrategy = SourceElementPositioningStrategies.DATA_MODIFIER,
        )

        val PokoOnValueClass by error0(
            positioningStrategy = SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER,
        )

        val PokoOnInnerClass by error0(
            positioningStrategy = SourceElementPositioningStrategies.INNER_MODIFIER,
        )

        val PrimaryConstructorRequired by error0(
            positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
        )

        val PrimaryConstructorPropertiesRequired by error0(
            positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
        )

        val SkippedPropertyWithCustomAnnotation by warning0(
            positioningStrategy = SourceElementPositioningStrategies.ANNOTATION_USE_SITE,
        )

        override val MAP = KtDiagnosticFactoryToRendererMap("Poko").apply {
            put(
                factory = PokoOnNonClass,
                message = "Poko can only be applied to a class",
            )
            put(
                factory = PokoOnDataClass,
                message = "Poko cannot be applied to a data class",
            )
            put(
                factory = PokoOnValueClass,
                message = "Poko cannot be applied to a value class",
            )
            put(
                factory = PokoOnInnerClass,
                message = "Poko cannot be applied to an inner class"
            )
            put(
                factory = PrimaryConstructorRequired,
                message = "Poko class must have a primary constructor"
            )
            put(
                factory = PrimaryConstructorPropertiesRequired,
                message = "Poko class primary constructor must have at least one not-skipped property",
            )
            put(
                factory = SkippedPropertyWithCustomAnnotation,
                message = "The @Skip annotation is experimental and its behavior may change; use with caution",
            )
        }

        init {
            RootDiagnosticRendererFactory.registerFactory(this)
        }

        /**
         * Copy of [org.jetbrains.kotlin.diagnostics.error0] with hack for correct `PsiElement`
         * class.
         */
        private fun error0(
            positioningStrategy: AbstractSourceElementPositioningStrategy = SourceElementPositioningStrategies.DEFAULT,
        ): DiagnosticFactory0DelegateProvider {
            return DiagnosticFactory0DelegateProvider(
                severity = Severity.ERROR,
                positioningStrategy = positioningStrategy,
                psiType = psiElementClass,
            )
        }

        /**
         * Copy of [org.jetbrains.kotlin.diagnostics.warning0] with hack for correct `PsiElement`
         * class.
         */
        private fun warning0(
            positioningStrategy: AbstractSourceElementPositioningStrategy = SourceElementPositioningStrategies.DEFAULT,
        ): DiagnosticFactory0DelegateProvider {
            return DiagnosticFactory0DelegateProvider(
                severity = Severity.WARNING,
                positioningStrategy = positioningStrategy,
                psiType = psiElementClass,
            )
        }
    }
}
