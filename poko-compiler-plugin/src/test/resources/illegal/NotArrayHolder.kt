package illegal

import dev.drewhamilton.poko.ExperimentalArrayContentSupport
import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.ArrayContentBased

@Suppress("Unused")
@OptIn(ExperimentalArrayContentSupport::class)
@Poko class NotArrayHolder(
    @ArrayContentBased val string: String,
    @ArrayContentBased val int: Int,
    @ArrayContentBased val float: Float,
)
