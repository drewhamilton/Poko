package dev.drewhamilton.poko

/**
 * Declares that a [Poko] class's generated functions will be based on this property's array
 * content. This differs from the Poko class (and data class) default of comparing arrays by
 * reference only.
 *
 * Poko class properties of type [Array], [BooleanArray], [CharArray], [ByteArray], [ShortArray],
 * [IntArray], [LongArray], [FloatArray], and [DoubleArray] are supported, including nested
 * [Array] types.
 *
 * Properties of a generic type or of type [Any] are also supported. For these properties, Poko will
 * generate a `when` statement that disambiguates the various array types at runtime and analyzes
 * content if the property is an array. (Note that with this logic, typed arrays will never be
 * considered equals to primitive arrays, even if they hold the same content. For example,
 * `arrayOf(1, 2)` will not be considered equals to `intArrayOf(1, 2)`.)
 *
 * Properties of a value class type that wraps an array are not supported. Tagging non-array
 * properties with this annotation is an error.
 *
 * Using array properties in data models is not generally recommended, because they are mutable.
 * Mutating an array marked with this annotation will cause the parent Poko class to produce
 * different `equals` and `hashCode` results at different times. This annotation should only be used
 * by consumers for whom performant code is more important than safe code.
 */
@ExperimentalArrayContentSupport
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class ArrayContentBased
