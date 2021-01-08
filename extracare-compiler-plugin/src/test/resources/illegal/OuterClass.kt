package illegal

import dev.drewhamilton.extracare.DataApi

@Suppress("unused")
class OuterClass {

    @DataApi inner class Inner(
        val value: String
    )
}
