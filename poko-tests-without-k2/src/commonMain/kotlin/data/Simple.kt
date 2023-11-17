package data

/**
 * A data class version of [poko.Simple], useful for comparing generated [toString], [equals], and [hashCode].
 */
@Suppress("unused")
data class Simple(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
)
