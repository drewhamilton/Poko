package illegal

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.ExperimentalArrayContentSupport
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@OptIn(ExperimentalArrayContentSupport::class)
@Poko class GenericArrayHolder<G : Collection<*>>(
    @ArrayContentBased val generic: G,
)
