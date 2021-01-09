package dev.drewhamilton.extracare.sample

import dev.drewhamilton.extracare.DataApi

@Suppress("unused")
@DataApi class Sample(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
)
