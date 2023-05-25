package dev.drewhamilton.poko

/**
 * Declares that a [Poko] class's generated functions will be based on this property's array
 * content. This differs from the Poko class (and data class) default of comparing arrays by
 * reference only.
 *
 * Poko class properties of type [Array], [BooleanArray], [ByteArray], [CharArray], [ShortArray],
 * [IntArray], [LongArray], [FloatArray], and [DoubleArray] are supported. Properties with a nested
 * [Array] type are not currently supported. Properties of a generic type or of type [Any] that are
 * assigned as arrays at runtime are not currently supported. Properties of a value class type that
 * wraps an array are not supported. Tagging non-array properties with this annotation is an error.
 *
 * Using array properties in data models is not generally recommended, because they are mutable.
 * Mutating an array marked with this annotation will cause the parent Poko class to produce
 * different `equals` and `hashCode` results at different times. This annotation should only be used
 * by consumers for whom performant code is more important than safe code.
 */
@ExperimentalArrayContentSupport
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class ArrayContentBased
