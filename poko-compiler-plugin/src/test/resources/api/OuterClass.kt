package api

import dev.drewhamilton.poko.Poko

@Suppress("unused")
class OuterClass {
    @Poko class Nested(
        val value: String
    )
}
