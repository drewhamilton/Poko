package api

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.ExperimentalArrayContentSupport
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@OptIn(ExperimentalArrayContentSupport::class)
@Poko class ComplexGenericArrayHolder<A : Any, G : A>(
    @ArrayContentBased val generic: G,
)
