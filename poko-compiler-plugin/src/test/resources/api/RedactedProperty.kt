package api

import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.RedactSupport

private const val HIDDEN = "[hidden]"

@OptIn(RedactSupport::class)
@Suppress("Unused")
@Poko class RedactedProperty(
    val id: String,
    @Poko.Redact val token: String,
    @Poko.Redact(mask = "<redacted>") val ssn: String?,
    @Poko.Redact(mask = HIDDEN) val constMask: String,
    @Poko.Redact(mask = "a" + "b") val concatMask: String,
) {
    @Poko.Redact val bodyProperty: String = "body"
}
