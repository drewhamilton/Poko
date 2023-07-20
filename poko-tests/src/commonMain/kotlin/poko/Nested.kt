package poko

import dev.drewhamilton.poko.Poko

class OuterClass {
    @Poko class Nested(
        val value: String
    )
}
