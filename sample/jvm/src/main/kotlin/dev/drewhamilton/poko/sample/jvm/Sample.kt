package dev.drewhamilton.poko.sample.jvm

import dev.drewhamilton.poko.PokoBuilder

@Suppress("unused")
@PokoBuilder
@MyData class Sample(
    val int: Int,
    val requiredString: String,
    val optionalString: String?,
)
