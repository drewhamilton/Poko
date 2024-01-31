package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
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
                declaration.classKind != ClassKind.CLASS ->
                    PokoErrors.POKO_ON_NON_CLASS_ERROR
                declaration.isData ->
                    PokoErrors.POKO_ON_DATA_CLASS_ERROR
                declaration.hasModifier(KtTokens.VALUE_KEYWORD) ->
                    PokoErrors.POKO_ON_VALUE_CLASS_ERROR
                declaration.isInner ->
                    PokoErrors.POKO_ON_INNER_CLASS_ERROR
                declaration.primaryConstructorIfAny(context.session) == null ->
                    PokoErrors.POKO_REQUIRES_PRIMARY_CONSTRUCTOR_ERROR
                else ->
                    null
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
                    factory = PokoErrors.POKO_REQUIRES_PRIMARY_CONSTRUCTOR_PROPERTIES_ERROR,
                    context = context,
                )
            }
        }
    }
}
