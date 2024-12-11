package dev.drewhamilton.poko

/**
 * Primary constructor properties marked with this annotation will be omitted from generated
 * `equals`, `hashCode`, and `toString` functions, as if they were not properties.
 *
 * This annotation has no effect on properties declared outside of the primary constructor.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
public annotation class Skipped
