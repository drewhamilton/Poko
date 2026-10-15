package poko

import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.RedactSupport

@OptIn(RedactSupport::class)
@Suppress("Unused")
@Poko class SkippedRedactedProperty(
    val id: String,
    @Poko.Skip @Poko.Redact val secret: String,
)
