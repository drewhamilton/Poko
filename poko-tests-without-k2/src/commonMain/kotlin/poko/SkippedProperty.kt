package poko

import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.Skipped
import dev.drewhamilton.poko.SkippedSupport

@OptIn(SkippedSupport::class)
@Suppress("Unused")
@Poko class SkippedProperty(
    val id: String,
    @Skipped val callback: () -> Unit,
)
