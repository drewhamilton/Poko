package illegal

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class GenericArrayHolder<G : Collection<*>>(
    @ArrayContentBased val generic: G,
)
