package illegal

import dev.drewhamilton.poko.Poko

@Poko class GenericArrayHolder<G : Collection<*>>(
    <!ReadArrayContentOnNonArrayProperty!>@Poko.ReadArrayContent val generic: G<!>,
)
