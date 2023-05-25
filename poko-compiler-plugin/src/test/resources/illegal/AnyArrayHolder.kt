package illegal

import dev.drewhamilton.poko.ExperimentalArrayContentSupport
import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.ReadArrayContent

@Suppress("Unused")
@OptIn(ExperimentalArrayContentSupport::class)
@Poko class AnyArrayHolder(
    @ReadArrayContent val any: Any?,
)
