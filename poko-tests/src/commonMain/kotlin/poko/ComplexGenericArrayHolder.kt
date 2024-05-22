package poko

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class ComplexGenericArrayHolder<A : Any, G : A>(
    @ArrayContentBased val generic: G,
)
