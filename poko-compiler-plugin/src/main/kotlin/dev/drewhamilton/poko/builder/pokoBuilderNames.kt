package dev.drewhamilton.poko.builder

import dev.drewhamilton.poko.unSpecial
import org.jetbrains.kotlin.name.Name

/**
 * Special marker [Name] for generated Builder `build` function. Used in FIR to disambiguate from a
 * possible consumer-written property named `build`.
 */
// TODO: Is it dangerous to abuse `special` like this?
internal val BuildFunctionSpecialName = Name.special("<build>")

/**
 * Actual [Name] of generated Builder `build` function. Used in IR to find the FIR-generated
 * declaration of this function.
 */
internal val BuildFunctionIdentifierName = BuildFunctionSpecialName.unSpecial()
