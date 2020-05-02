package dev.drewhamilton.extracare.sample.explicit

import dev.drewhamilton.extracare.DataApi

@DataApi class ExplicitFunctionDeclarations(
    private val string: String
) {
    override fun toString() = string
    override fun equals(other: Any?) = other == true
    override fun hashCode() = string.hashCode()
}
