package illegal

import dev.drewhamilton.poko.Poko

@Poko class GenericArrayHolder<G : Collection<*>>(
    @Poko.ReadArrayContent val generic: G,
)
