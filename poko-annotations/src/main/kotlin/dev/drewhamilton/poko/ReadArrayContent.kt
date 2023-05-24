package dev.drewhamilton.poko

/**
 * Tags an [Array], [BooleanArray], [ByteArray], [CharArray], [ShortArray], [IntArray], [LongArray],
 * [FloatArray], or [DoubleArray] property in a [Poko] class as one whose content will be read when
 * computing [equals], [hashCode], and [toString].
 *
 * Like data classes, [Poko] classes do not read array content by default. Using array properties in
 * data models is not generally recommended, because they are mutable. Mutating an array marked with
 * this annotation will cause the parent Poko class to produce different `equals` and `hashCode`
 * results at different times. This annotation should only be used by consumers for whom performant
 * code is more important than safe code.
 *
 * Tagging any other property type with this annotation is an error.
 *
 * Nested arrays are not currently supported. Properties of a generic type or of type [Any] that are
 * assigned as arrays at runtime are not currently supported.
 */
@ExperimentalArrayContentSupport
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class ReadArrayContent
