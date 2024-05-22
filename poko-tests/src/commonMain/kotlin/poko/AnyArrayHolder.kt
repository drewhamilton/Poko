package poko

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class AnyArrayHolder(
    @ArrayContentBased val any: Any,
    @ArrayContentBased val nullableAny: Any?,
    val trailingProperty: String,
)
