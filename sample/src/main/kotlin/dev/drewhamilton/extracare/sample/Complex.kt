package dev.drewhamilton.extracare.sample

import dev.drewhamilton.extracare.DataApi

@DataApi class Complex<T>(
    val referenceType: String,
    val nullableReferenceType: String?,
    val int: Int,
    val nullableInt: Int?,
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
