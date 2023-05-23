package dev.drewhamilton.poko

/**
 * Tags an [Array], [BooleanArray], [ByteArray], [CharArray], [ShortArray], [IntArray], [FloatArray], or [DoubleArray]
 * property in a [Poko] class as one whose content will be considered when computing [equals], [hashCode], and
 * [toString]. Like data classes, [Poko] does not inspect array content by default.
 *
 * Typed [Array]s inspect content deeply, so nested [Array]s are supported.
 *
 * Tagging any other property type with this annotation is an error. Properties of type [Any] that are arrays at runtime
 * are not currently supported.
 */
@ExperimentalArrayContentSupport
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class ArrayContent
