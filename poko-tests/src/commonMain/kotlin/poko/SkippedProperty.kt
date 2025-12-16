package poko

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class SkippedProperty(
    val id: String,
    @Poko.Skip val callback: () -> Unit,
)
