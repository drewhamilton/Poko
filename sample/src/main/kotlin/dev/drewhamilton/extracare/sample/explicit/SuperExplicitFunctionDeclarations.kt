package dev.drewhamilton.extracare.sample.explicit

open class SuperExplicitFunctionDeclarations(
    private val string: String
) {
    override fun toString() = string
    override fun equals(other: Any?) = other == true
    override fun hashCode() = string.hashCode()
}
