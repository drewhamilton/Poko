package api

import dev.drewhamilton.extracare.DataApi
import java.util.Objects

@Suppress("Unused")
@DataApi class ExplicitDeclarations(
    private val string: String
) {
    override fun toString() = string
    override fun equals(other: Any?) = other is ExplicitDeclarations && other.string == string
    override fun hashCode() = Objects.hash(string)
}
