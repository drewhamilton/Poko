package dev.drewhamilton.poko.sample

/**
 * Annotation used for Poko compiler plugin, which generates [equals], [hashCode], and [toString] for the annotated
 * class.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Poko
