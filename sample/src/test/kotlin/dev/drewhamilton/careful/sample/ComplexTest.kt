package dev.drewhamilton.careful.sample

import com.google.common.truth.Truth.assertThat
import dev.drewhamilton.careful.sample.alt.DataComplex
import org.junit.Test

class ComplexTest {

    @Test fun `equals works with same array instances`() {
        // TODO: Document that array members are not supported, or support them
        val arrayReferenceType = arrayOf("B", "C")
        val arrayPrimitiveType = intArrayOf(324, 23423)
        val genericCollectionType = listOf(false, true, true)
        val a = Complex(
            referenceType = "A",
            nullableReferenceType = null,
            primitiveType = 19,
            nullablePrimitiveType = null,
            arrayReferenceType = arrayReferenceType,
            nullableArrayReferenceType = null,
            arrayPrimitiveType = arrayPrimitiveType,
            nullableArrayPrimitiveType = null,
            genericCollectionType = genericCollectionType,
            nullableGenericCollectionType = null,
            genericType = true,
            nullableGenericType = null
        )
        val b = Complex(
            referenceType = "A",
            nullableReferenceType = null,
            primitiveType = 19,
            nullablePrimitiveType = null,
            arrayReferenceType = arrayReferenceType,
            nullableArrayReferenceType = null,
            arrayPrimitiveType = arrayPrimitiveType,
            nullableArrayPrimitiveType = null,
            genericCollectionType = genericCollectionType,
            nullableGenericCollectionType = null,
            genericType = true,
            nullableGenericType = null
        )

        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun `hashCode is consistent`() {
        val a = Complex(
            referenceType = "A",
            nullableReferenceType = null,
            primitiveType = 19,
            nullablePrimitiveType = null,
            arrayReferenceType = arrayOf("B", "C"),
            nullableArrayReferenceType = null,
            arrayPrimitiveType = intArrayOf(324, 23423),
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(false, true, true),
            nullableGenericCollectionType = null,
            genericType = true,
            nullableGenericType = null
        )
        val b = Complex(
            referenceType = "A",
            nullableReferenceType = null,
            primitiveType = 19,
            nullablePrimitiveType = null,
            arrayReferenceType = arrayOf("B", "C"),
            nullableArrayReferenceType = null,
            arrayPrimitiveType = intArrayOf(324, 23423),
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(false, true, true),
            nullableGenericCollectionType = null,
            genericType = true,
            nullableGenericType = null
        )

        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test fun `hashCode is equivalent to data class hashCode`() {
        val careful = Complex(
            referenceType = "A",
            nullableReferenceType = null,
            primitiveType = 19,
            nullablePrimitiveType = null,
            arrayReferenceType = arrayOf("B", "C"),
            nullableArrayReferenceType = null,
            arrayPrimitiveType = intArrayOf(324, 23423),
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(false, true, true),
            nullableGenericCollectionType = null,
            genericType = true,
            nullableGenericType = null
        )

        val data = DataComplex(
            referenceType = "A",
            nullableReferenceType = null,
            primitiveType = 19,
            nullablePrimitiveType = null,
            arrayReferenceType = arrayOf("B", "C"),
            nullableArrayReferenceType = null,
            arrayPrimitiveType = intArrayOf(324, 23423),
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(false, true, true),
            nullableGenericCollectionType = null,
            genericType = true,
            nullableGenericType = null
        )

        assertThat(careful.hashCode()).isEqualTo(data.hashCode())
    }

    // TODO MISSING: toString test
}
