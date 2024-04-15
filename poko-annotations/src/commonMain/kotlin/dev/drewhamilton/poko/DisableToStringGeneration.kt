package dev.drewhamilton.poko

/**
 * Declares that a [Poko] class will not have [Any.toString] method generated.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class DisableToStringGeneration
