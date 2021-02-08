package api

import dev.drewhamilton.poko.Poko

@Suppress("unused")
interface OuterInterface {
    @Poko class Nested(
        val value: String
    )
}
