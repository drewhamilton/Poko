package poko

import dev.drewhamilton.poko.IndependentFunctionsSupport
import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.RedactSupport

@OptIn(IndependentFunctionsSupport::class, RedactSupport::class)
@Poko.EqualsAndHashCode class OnlyEqualsAndHashCodeRedacted(
    val id: Long,
    @Poko.Redact val secret: String,
)
