package dev.drewhamilton.poko

/**
 * Denotes an experimental API that allows generating Poko functions individually; i.e. a class
 * could have `toString` or it could have `equals` and `hashCode`, without having all three.
 */
@RequiresOptIn
public annotation class IndependentFunctionsSupport

/**
 * Denotes an experimental API that enables the ability to skip a Poko class primary constructor
 * property when generating Poko functions.
 */
@RequiresOptIn
public annotation class SkipSupport

/**
 * Denotes an experimental API that enables support for array content reading.
 */
@Deprecated(
    message = "Array content support no longer requires opt-in",
    level = DeprecationLevel.ERROR,
)
@RequiresOptIn
public annotation class ArrayContentSupport
