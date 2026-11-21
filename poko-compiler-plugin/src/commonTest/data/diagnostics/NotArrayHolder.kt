package illegal

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class NotArrayHolder(
    <!ReadArrayContentOnNonArrayProperty!>@Poko.ReadArrayContent val string: String<!>,
    <!ReadArrayContentOnNonArrayProperty!>@Poko.ReadArrayContent val int: Int<!>,
    <!ReadArrayContentOnNonArrayProperty!>@Poko.ReadArrayContent val float: Float<!>,
)
