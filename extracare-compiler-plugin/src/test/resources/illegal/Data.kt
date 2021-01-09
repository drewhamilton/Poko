package illegal

import dev.drewhamilton.extracare.DataApi

@Suppress("Unused")
@DataApi data class Data(
    val string: String,
    val float: Float,
    val double: Double,
    val long: Long,
    val int: Int,
    val short: Short,
    val byte: Byte,
    val boolean: Boolean
)