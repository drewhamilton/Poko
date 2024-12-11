package poko

import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.Skipped

@Suppress("Unused")
@Poko class SkippedProperty(
    val id: String,
    @Skipped val callback: () -> Unit,
)
