package api

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class SimpleWithExtraParam(
    val int: Int,
    val requiredString: String,
    val optionalString: String?,
    callback: (Unit) -> Boolean,
) {
    @Suppress("CanBePrimaryConstructorProperty")
    val callback: (Unit) -> Boolean = callback
}
