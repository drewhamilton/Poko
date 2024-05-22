package illegal

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class NotArrayHolder(
    @ArrayContentBased val string: String,
    @ArrayContentBased val int: Int,
    @ArrayContentBased val float: Float,
)
