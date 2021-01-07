package api

import dev.drewhamilton.extracare.DataApi

@Suppress("Unused")
@DataApi class Simple(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
)
