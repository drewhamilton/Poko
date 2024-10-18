package dev.drewhamilton.poko.fir

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId

internal class PokoFirExtensionSessionComponent(
    session: FirSession,
    private val pokoAnnotation: ClassId,
) : FirExtensionSessionComponent(session) {
    fun pokoAnnotation(declaration: FirDeclaration): FirAnnotation? {
        return declaration.annotations.firstOrNull { firAnnotation ->
            firAnnotation.classId() == pokoAnnotation
        }
    }

    private fun FirAnnotation.classId(): ClassId? {
        val coneClassLikeType = try {
            annotationTypeRef.coneTypeSafe<ConeClassLikeType>()
        } catch (noSuchMethodError: NoSuchMethodError) {
            val annotationTypeRef = annotationTypeRef
            if (annotationTypeRef is FirResolvedTypeRef) {
                // The `coneTypeSafe` inline function uses a getter that changes names from `type`
                // to `coneType` in 2.1+, so we access the latter via reflection here:
                FirResolvedTypeRef::class.java
                    .methods
                    .single { it.name == "getConeType" }
                    .invoke(annotationTypeRef)
                    as? ConeClassLikeType
            } else {
                null
            }
        }
        return coneClassLikeType?.classId
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun <reified T : ConeKotlinType> FirTypeRef.coneTypeSafeReflection(): T? {
        contract {
            returnsNotNull() implies (this@coneTypeSafeReflection is FirResolvedTypeRef)
        }
        return (this as? FirResolvedTypeRef)?.type as? T
    }

    internal companion object {
        internal fun getFactory(pokoAnnotation: ClassId): Factory {
            return Factory { session ->
                PokoFirExtensionSessionComponent(session, pokoAnnotation)
            }
        }
    }
}

internal val FirSession.pokoFirExtensionSessionComponent: PokoFirExtensionSessionComponent by FirSession.sessionComponentAccessor()
