package dev.drewhamilton.poko.sample.mpp

import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.RedactSupport

@OptIn(RedactSupport::class)
@Suppress("unused")
@Poko class RedactSample(
    val id: String,
    @Poko.Redact val token: String,
    @Poko.Redact(mask = "<redacted>") val ssn: String?,
)
