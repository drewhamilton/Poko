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
public annotation class Poko(
    val excludeFunctions: Boolean = false
)
