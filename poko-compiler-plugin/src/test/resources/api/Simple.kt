package api

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class Simple(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
)
