package dev.drewhamilton.poko

/**
 * A `@Poko class` is similar to a `data class`: the Poko compiler plugin will generate [equals],
 * [hashCode], and [toString] functions for any Kotlin class marked with this annotation. Unlike
 * normal data classes, `copy` or `componentN` functions are not generated. This makes it easier to
 * maintain data models in libraries without breaking binary compatibility.
 *
 * The generated functions will be based on class properties in the primary constructor. Class
 * properties not in the primary constructor will not be included, and primary constructor
 * parameters that are not class properties will not be included. Compilation will fail if the
 * annotated class does not include a primary constructor.
 *
 * Each function will only be generated if it is not already manually overridden in the annotated
 * class.
 *
 * The annotated class cannot be a `data class`, an `inline class`, or an `inner class`.
 *
 * Like data classes, it is highly recommended that all properties used in equals/hashCode are
 * immutable. `var`s, mutable collections, and especially arrays should be avoided. The class itself
 * should also be final. The compiler plugin does not enforce these recommendations.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class Poko {

    /**
     * Generates the [equals] and [hashCode] functions of a class without generating the `toString`
     * function. See further documentation on the [Poko] annotation.
     */
    @IndependentFunctionsSupport
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.CLASS)
    public annotation class EqualsAndHashCode

    /**
     * Generates the [toString] function of a class without generating the `equals` or `hashCode`
     * functions. See further documentation on the [Poko] annotation.
     */
    @IndependentFunctionsSupport
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.CLASS)
    public annotation class ToString

    /**
     * Primary constructor properties marked with this annotation will be omitted from generated
     * `equals`, `hashCode`, and `toString` functions, as if they were not properties.
     *
     * This annotation has no effect on properties declared outside the primary constructor.
     */
    @SkipSupport
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.PROPERTY)
    public annotation class Skip

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
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.PROPERTY)
    public annotation class ReadArrayContent
}
