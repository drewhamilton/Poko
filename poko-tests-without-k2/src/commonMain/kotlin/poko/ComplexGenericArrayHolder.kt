package poko

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class ComplexGenericArrayHolder<A : Any, G : A>(
    @Poko.ReadArrayContent val generic: G,
)
