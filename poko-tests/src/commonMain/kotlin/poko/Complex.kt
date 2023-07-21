package poko

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class Complex<T>(
    val referenceType: String,
    val nullableReferenceType: String?,
    val int: Int,
    val nullableInt: Int?,
    val long: Long,
    val float: Float,
    val double: Double,
    val arrayReferenceType: Array<String>,
    val nullableArrayReferenceType: Array<String>?,
    val arrayPrimitiveType: IntArray,
    val nullableArrayPrimitiveType: IntArray?,
    val genericCollectionType: List<T>,
    val nullableGenericCollectionType: List<T>?,
    val genericType: T,
    val nullableGenericType: T?
)
