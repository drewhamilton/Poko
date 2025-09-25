package poko

import dev.drewhamilton.poko.IndependentFunctionsSupport
import dev.drewhamilton.poko.Poko

@OptIn(IndependentFunctionsSupport::class)
@Poko.ToString class OnlyToString(
    val name: String,
)
