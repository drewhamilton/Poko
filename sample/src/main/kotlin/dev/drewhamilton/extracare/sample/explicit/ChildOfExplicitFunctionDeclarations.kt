package dev.drewhamilton.extracare.sample.explicit

import dev.drewhamilton.extracare.DataApi

@DataApi class ChildOfExplicitFunctionDeclarations(
    private val string: String
) : SuperExplicitFunctionDeclarations(string)
