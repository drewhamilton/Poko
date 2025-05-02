package dev.drewhamilton.poko.sample.mpp

import dev.drewhamilton.poko.Poko

@Suppress("unused")
@Poko class ArraysSample(
    @Poko.ReadArrayContent val primitive: ByteArray,
    @Poko.ReadArrayContent val standard: Array<String>,
    @Poko.ReadArrayContent val nested: Array<CharArray>,
    @Poko.ReadArrayContent val runtime: Any,
)
