package dev.drewhamilton.extracare.sample

import com.google.common.truth.Truth.assertThat
import dev.drewhamilton.extracare.sample.alt.DataComplex
import org.junit.Test

class ComplexTest {

    @Test fun `equals works with same array instances`() {
        val arrayReferenceType = arrayOf("B", "C")
        val arrayPrimitiveType = intArrayOf(324, 23423)
        val genericCollectionType = listOf(false, true, true)
        val a = Complex(
            referenceType = "A",
            nullableReferenceType = null,
            int = 19,
            nullableInt = null,
            long = 2222222222L,
            float = 20f,
            double = 21.0,
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
            int = 19,
            nullableInt = null,
            long = 2222222222L,
            float = 20f,
            double = 21.0,
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
            int = 19,
            nullableInt = null,
            long = 2222222222L,
            float = 20f,
            double = 21.0,
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
            int = 19,
            nullableInt = null,
            long = 2222222222L,
            float = 20f,
            double = 21.0,
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
        val dataApi = Complex(
            referenceType = "A",
            nullableReferenceType = null,
            int = 19,
            nullableInt = null,
            long = 2222222222L,
            float = 20f,
            double = 21.0,
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
            int = 19,
            nullableInt = null,
            long = 2222222222L,
            float = 20f,
            double = 21.0,
            arrayReferenceType = arrayOf("B", "C"),
            nullableArrayReferenceType = null,
            arrayPrimitiveType = intArrayOf(324, 23423),
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(false, true, true),
            nullableGenericCollectionType = null,
            genericType = true,
            nullableGenericType = null
        )

        assertThat(dataApi.hashCode()).isEqualTo(data.hashCode())
    }

    @Test fun `toString includes class name and each property`() {
        val complex = Complex(
            referenceType = "sample",
            nullableReferenceType = null,
            int = 19,
            nullableInt = null,
            long = 2222222222L,
            float = 20f,
            double = 21.0,
            arrayReferenceType = arrayOf("one", "two"),
            nullableArrayReferenceType = null,
            arrayPrimitiveType = intArrayOf(1234, 5678),
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(true, false, true),
            nullableGenericCollectionType = null,
            genericType = false,
            nullableGenericType = null
        )
        assertThat(complex.toString()).isEqualTo(
            "Complex(" +
                    "referenceType=sample, " +
                    "nullableReferenceType=null, " +
                    "int=19, " +
                    "nullableInt=null, " +
                    "long=2222222222, " +
                    "float=20.0, " +
                    "double=21.0, " +
                    "arrayReferenceType=[one, two], " +
                    "nullableArrayReferenceType=null, " +
                    "arrayPrimitiveType=[1234, 5678], " +
                    "nullableArrayPrimitiveType=null, " +
                    "genericCollectionType=[true, false, true], " +
                    "nullableGenericCollectionType=null, " +
                    "genericType=false, " +
                    "nullableGenericType=null" +
                    ")"
        )
    }

    @Test fun `toString is equivalent to data class toString`() {
        val dataApi = Complex(
            referenceType = "string",
            nullableReferenceType = null,
            int = 19,
            nullableInt = null,
            long = 2222222222L,
            float = 20f,
            double = 21.0,
            arrayReferenceType = arrayOf("one", "two"),
            nullableArrayReferenceType = null,
            arrayPrimitiveType = intArrayOf(1234, 5678),
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(true, false, true),
            nullableGenericCollectionType = null,
            genericType = false,
            nullableGenericType = null
        )

        val data = DataComplex(
            referenceType = "string",
            nullableReferenceType = null,
            int = 19,
            nullableInt = null,
            long = 2222222222L,
            float = 20f,
            double = 21.0,
            arrayReferenceType = arrayOf("one", "two"),
            nullableArrayReferenceType = null,
            arrayPrimitiveType = intArrayOf(1234, 5678),
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(true, false, true),
            nullableGenericCollectionType = null,
            genericType = false,
            nullableGenericType = null
        )

        assertThat(dataApi.toString()).isEqualTo(data.toString().removePrefix("Data"))
    }
}
