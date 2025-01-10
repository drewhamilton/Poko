package dev.drewhamilton.poko

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Exhaustive representation of all functions Poko generates.
 */
internal enum class PokoFunction(
    val functionName: Name,
) {
    Equals(OperatorNameConventions.EQUALS),
    HashCode(OperatorNameConventions.HASH_CODE),
    ToString(OperatorNameConventions.TO_STRING),
}
