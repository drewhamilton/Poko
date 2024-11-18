package poko

import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.PokoSkip

@Suppress("Unused")
@Poko class SkippedProperty(
    val id: String,
    @PokoSkip val callback: () -> Unit,
)
