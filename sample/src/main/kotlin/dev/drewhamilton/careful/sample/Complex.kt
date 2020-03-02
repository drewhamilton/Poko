package dev.drewhamilton.careful.sample

import dev.drewhamilton.careful.Careful

@Careful class Complex<T>(
    val referenceType: String,
    val nullableReferenceType: String?,
    val primitiveType: Int,
    val nullablePrimitiveType: Int?,
    val arrayReferenceType: Array<String>,
    val nullableArrayReferenceType: Array<String>?,
    val arrayPrimitiveType: IntArray,
    val nullableArrayGenericType: IntArray?,
    val genericCollectionType: List<T>,
    val nullableGenericCollectionType: List<T>?,
    val genericType: T,
    val nullableGenericType: T?
)
