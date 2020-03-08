package dev.drewhamilton.careful.sample.alt

/**
 * A data class version of [dev.drewhamilton.careful.sample.Simple], useful for inspecting target bytecode for
 * [toString], [equals], and [hashCode].
 */
@Suppress("unused")
data class DataSimple(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
)
