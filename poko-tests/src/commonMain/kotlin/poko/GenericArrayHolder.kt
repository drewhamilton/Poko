package poko

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class GenericArrayHolder<G>(
    @ArrayContentBased val generic: G,
)
