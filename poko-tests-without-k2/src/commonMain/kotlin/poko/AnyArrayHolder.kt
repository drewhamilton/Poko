package poko

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class AnyArrayHolder(
    @Poko.ReadArrayContent val any: Any,
    @Poko.ReadArrayContent val nullableAny: Any?,
    val trailingProperty: String,
)
