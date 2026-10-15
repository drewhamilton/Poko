package poko

import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.RedactSupport

@OptIn(RedactSupport::class)
@Suppress("Unused")
@Poko class RedactedProperty(
    val id: String,
    @Poko.Redact val token: String,
    @Poko.Redact(mask = "<redacted>") val ssn: String?,
    @Poko.Redact @Poko.ReadArrayContent val bytes: ByteArray,
    @Poko.Redact(mask = "") val note: String,
)
