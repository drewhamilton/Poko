package dev.drewhamilton.careful.sample

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ComplexTest {

    @Test fun `equals works`() {
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

        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }
}
