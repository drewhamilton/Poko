package api

import dev.drewhamilton.extracare.DataApi

@Suppress("unused")
class OuterClass {
    @DataApi class Nested(
        val value: String
    )
}
