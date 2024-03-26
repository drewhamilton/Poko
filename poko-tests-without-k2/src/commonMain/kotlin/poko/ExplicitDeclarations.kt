package poko

import dev.drewhamilton.poko.Poko

@Poko class ExplicitDeclarations(
    private val string: String
) {
    override fun toString() = string
    override fun equals(other: Any?) = other is ExplicitDeclarations && other.string.length == string.length
    override fun hashCode() = string.length
}
