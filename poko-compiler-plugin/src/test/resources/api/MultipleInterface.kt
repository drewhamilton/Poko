package api

import dev.drewhamilton.poko.Poko

@Poko
class MultipleInterface(
    val int: Int,
    val requiredString: String,
    val optionalString: String?,
): FunctionalInterface, MarkerInterface {
    override fun getAnInt(): Int = int
}

interface FunctionalInterface {
    fun getAnInt(): Int
}

interface MarkerInterface
