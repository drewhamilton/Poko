package dev.drewhamilton.careful.sample

import dev.drewhamilton.careful.Careful

@Careful class Simple(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
)
