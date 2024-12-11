package dev.drewhamilton.poko

/**
 * Denotes an experimental API that enables the ability to skip a Poko class primary constructor property when
 * generating Poko functions.
 */
@RequiresOptIn
public annotation class SkippedSupport

/**
 * Denotes an API that enables support for array content reading, which is experimental and may
 * change or break.
 */
@Deprecated("Array content support no longer requires opt-in")
@RequiresOptIn
public annotation class ArrayContentSupport
