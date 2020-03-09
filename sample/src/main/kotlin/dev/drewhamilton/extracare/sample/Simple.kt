package dev.drewhamilton.extracare.sample

import dev.drewhamilton.extracare.DataApi

@DataApi class Simple(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
)
