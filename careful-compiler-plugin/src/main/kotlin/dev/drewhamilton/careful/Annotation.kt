package dev.drewhamilton.careful

import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.name.FqName

// TODO: Centralize
internal val carefulAnnotationName = FqName("dev.drewhamilton.careful.Careful")

internal val Annotated.isCareful: Boolean
    get() = annotations.hasAnnotation(carefulAnnotationName)
