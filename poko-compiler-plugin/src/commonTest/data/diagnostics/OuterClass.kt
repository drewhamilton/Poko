package illegal

import dev.drewhamilton.poko.Poko

@Suppress("unused")
class OuterClass {

    @Poko <!PokoOnInnerClass!>inner<!> class Inner(
        val value: String
    )
}
