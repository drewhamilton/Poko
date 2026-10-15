package poko

import dev.drewhamilton.poko.IndependentFunctionsSupport
import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.RedactSupport

@OptIn(IndependentFunctionsSupport::class, RedactSupport::class)
@Poko.ToString class OnlyToStringRedacted(
    val name: String,
    @Poko.Redact val secret: String,
)
