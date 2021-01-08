package api

import dev.drewhamilton.extracare.DataApi

@Suppress("unused")
interface OuterInterface {
    @DataApi class Nested(
        val value: String
    )
}
