package dev.drewhamilton.extracare

import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.name.FqName

// TODO: Centralize
internal val dataApiAnnotationName = FqName("dev.drewhamilton.extracare.DataApi")

internal val Annotated.isDataApi: Boolean
    get() = annotations.hasAnnotation(dataApiAnnotationName)
