package poko

import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.RedactSupport

@OptIn(RedactSupport::class)
@Suppress("Unused")
@Poko class RedactedFirstProperty(
    @Poko.Redact val secret: String,
    val id: String,
)
