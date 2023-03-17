package dev.drewhamilton.poko.sample.compose

/**
 * Annotation used for Poko compiler plugin, which generates [equals], [hashCode], and [toString] for the annotated
 * class.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Poko
