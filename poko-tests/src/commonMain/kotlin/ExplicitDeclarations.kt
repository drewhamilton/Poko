import dev.drewhamilton.poko.Poko

@Poko class ExplicitDeclarationsPoko(
    private val string: String
) {
    override fun toString() = string
    override fun equals(other: Any?) = other is ExplicitDeclarationsPoko && other.string.length == string.length
    override fun hashCode() = string.length
}

data class ExplicitDeclarationsData(
    private val string: String
) {
    override fun toString() = string
    override fun equals(other: Any?) = other is ExplicitDeclarationsData && other.string.length == string.length
    override fun hashCode() = string.length
}
