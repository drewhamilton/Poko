package poko

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class GenericArrayHolder<G>(
    @Poko.ReadArrayContent val generic: G,
)
