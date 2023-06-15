package illegal

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.ArrayContentSupport
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@OptIn(ArrayContentSupport::class)
@Poko class GenericArrayHolder<G : Collection<*>>(
    @ArrayContentBased val generic: G,
)
