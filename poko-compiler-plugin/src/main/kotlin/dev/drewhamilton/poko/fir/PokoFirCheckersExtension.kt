package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.lexer.KtTokens

internal class PokoFirCheckersExtension(
    session: FirSession,
) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers =
        object : DeclarationCheckers() {
            override val regularClassCheckers: Set<FirRegularClassChecker> =
                setOf(PokoFirRegularClassChecker)
        }

    internal object PokoFirRegularClassChecker : FirRegularClassChecker() {
        override fun check(
            declaration: FirRegularClass,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            val matcher = context.session.pokoFirExtensionSessionComponent
            if (matcher.pokoAnnotation(declaration) == null) return

            val errorFactory = when {
                declaration.classKind != ClassKind.CLASS -> Errors.PokoOnNonClass
                declaration.isData -> Errors.PokoOnDataClass
                declaration.hasModifier(KtTokens.VALUE_KEYWORD) -> Errors.PokoOnValueClass
                declaration.isInner -> Errors.PokoOnInnerClass
                declaration.primaryConstructorIfAny(context.session) == null ->
                    Errors.PrimaryConstructorRequired
                else -> null
            }
            if (errorFactory != null) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = errorFactory,
                    context = context,
                )
            }

            val constructorProperties = declaration.declarations
                .filterIsInstance<FirProperty>()
                .filter {
                    it.source?.kind is KtFakeSourceElementKind.PropertyFromParameter
                }
            if (constructorProperties.isEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = Errors.PrimaryConstructorPropertiesRequired,
                    context = context,
                )
            }
        }
    }

    private object Errors : BaseDiagnosticRendererFactory() {

        val PokoOnNonClass by error0<PsiElement>(
            positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
        )

        val PokoOnDataClass by error0<PsiElement>(
            positioningStrategy = SourceElementPositioningStrategies.DATA_MODIFIER,
        )

        val PokoOnValueClass by error0<PsiElement>(
            positioningStrategy = SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER,
        )

        val PokoOnInnerClass by error0<PsiElement>(
            positioningStrategy = SourceElementPositioningStrategies.INNER_MODIFIER,
        )

        val PrimaryConstructorRequired by error0<PsiElement>(
            positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
        )

        val PrimaryConstructorPropertiesRequired by error0<PsiElement>(
            positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
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
                message = "Poko class primary constructor must have at least one property",
            )
        }

        init {
            RootDiagnosticRendererFactory.registerFactory(this)
        }
    }
}
