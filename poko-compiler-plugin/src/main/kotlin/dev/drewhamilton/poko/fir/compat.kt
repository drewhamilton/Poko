package dev.drewhamilton.poko.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.plugin.SimpleFunctionBuildingContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

internal fun FirExtension.createMemberFunctionCompat(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirNamedFunctionCompat {
    val value = try {
        createMemberFunction(
            owner = owner,
            key = key,
            name = name,
            returnType = returnType,
            config = config
        )
    } catch (_: NoSuchMethodError) {
        // 2.3.0:
        javaClass.classLoader
            .loadClass("org.jetbrains.kotlin.fir.plugin.SimpleFunctionBuildingContextKt")
            .methods
            .single { it.name == "createMemberFunction" &&
                it.parameters.size == 6 &&
                it.parameters[0].type == FirExtension::class.java &&
                it.parameters[1].type == FirClassSymbol::class.java &&
                it.parameters[2].type == GeneratedDeclarationKey::class.java &&
                it.parameters[3].type == Name::class.java &&
                it.parameters[4].type == ConeKotlinType::class.java &&
                it.parameters[5].type.name == "kotlin.jvm.functions.Function1"
            }
            .invoke(
                null, // Static invocation
                this, // Extension receiver
                owner,
                key,
                name,
                returnType,
                config,
            )
    }
    return FirNamedFunctionCompat(value)
}

/**
 * Wrapper for [FirNamedFunction], which is renamed from `FirSimpleFunction` in Kotlin 2.3.20. Uses
 * reflection to resolve needed properties and functions if resolving them directly fails.
 */
@JvmInline
internal value class FirNamedFunctionCompat(
    val value: Any,
) {
    val symbol: FirNamedFunctionSymbol
        get() = try {
            (value as FirNamedFunction).symbol
        } catch (_: NoClassDefFoundError) {
            // 2.3.0:
            javaClass.classLoader
                .loadClass("org.jetbrains.kotlin.fir.declarations.FirSimpleFunction")
                .methods
                .single {
                    it.name == "getSymbol" &&
                        it.parameters.isEmpty() &&
                        it.returnType == FirNamedFunctionSymbol::class.java
                }
                .invoke(
                    value, // Dispatch receiver
                )
                as FirNamedFunctionSymbol
        }
}
