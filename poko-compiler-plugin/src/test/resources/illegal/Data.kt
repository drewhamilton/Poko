package illegal

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko data class Data(
    val string: String,
    val float: Float,
    val double: Double,
    val long: Long,
    val int: Int,
    val short: Short,
    val byte: Byte,
    val boolean: Boolean
)
