package poko

import dev.drewhamilton.poko.Poko

open class SuperclassWithFinalOverrides(
    private val id: String,
) {
    final override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is SuperclassWithFinalOverrides -> false
        else -> this.id == other.id
    }

    final override fun hashCode(): Int = 31 + id.hashCode()

    final override fun toString(): String = id

    @Poko class Subclass(
        val name: String,
    ) : SuperclassWithFinalOverrides(id = "Subclass")
}

