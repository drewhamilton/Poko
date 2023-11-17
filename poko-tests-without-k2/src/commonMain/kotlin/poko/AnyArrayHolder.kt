package poko

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.ArrayContentSupport
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@OptIn(ArrayContentSupport::class)
@Poko class AnyArrayHolder(
    @ArrayContentBased val any: Any,
    @ArrayContentBased val nullableAny: Any?,
    val trailingProperty: String,
)
