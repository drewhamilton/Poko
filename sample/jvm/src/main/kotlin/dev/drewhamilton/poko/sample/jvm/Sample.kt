package dev.drewhamilton.poko.sample.jvm

import dev.drewhamilton.poko.Poko

@Suppress("unused")
@Poko class Sample(
    val int: Int,
    val requiredString: String,
    val optionalString: String?,
)
