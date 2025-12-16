package illegal

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class NotArrayHolder(
    @Poko.ReadArrayContent val string: String,
    @Poko.ReadArrayContent val int: Int,
    @Poko.ReadArrayContent val float: Float,
)
