package dev.drewhamilton.extracare.sample

/**
 * Annotation used for Extra Care compiler plugin, which generates [equals], [hashCode], and [toString] for the
 * annotated class.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Poko
