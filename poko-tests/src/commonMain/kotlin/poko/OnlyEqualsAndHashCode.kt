package poko

import dev.drewhamilton.poko.IndependentFunctionsSupport
import dev.drewhamilton.poko.Poko

@OptIn(IndependentFunctionsSupport::class)
@Poko.EqualsAndHashCode class OnlyEqualsAndHashCode(
    val id: Long,
)
