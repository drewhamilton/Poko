// POKO_ANNOTATION_NAME: nonexistent/ClassName
package api

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class Primitives(
    val string: String,
    val float: Float,
    val double: Double,
    val long: Long,
    val int: Int,
    val short: Short,
    val byte: Byte,
    val boolean: Boolean
)

fun box(): String { return "OK" }
