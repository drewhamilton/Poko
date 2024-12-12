package poko

import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.SkipSupport

@OptIn(SkipSupport::class)
@Suppress("Unused")
@Poko class SkippedProperty(
    val id: String,
    @Poko.Skip val callback: () -> Unit,
)
