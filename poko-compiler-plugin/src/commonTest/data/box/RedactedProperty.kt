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

fun box(): String {
    val instance = RedactedProperty("a", "secret-token", "123-45-6789", "c", "d")
    return if ("$instance" == "RedactedProperty(id=a, token=***, ssn=<redacted>, constMask=[hidden], concatMask=ab)")
        "OK"
    else "$instance"
}
