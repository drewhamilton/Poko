import assertk.assertAll
import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.toStringFun
import kotlin.test.Test
import poko.Complex as ComplexPoko

class ComplexTest {
    @Test fun two_equivalent_compiled_Complex_instances_match() {
        val arrayReferenceType = arrayOf("one string", "another string")
        val arrayPrimitiveType = intArrayOf(3, 4, 5)
        val a = ComplexPoko(
            referenceType = "Text",
            nullableReferenceType = null,
            int = 2,
            nullableInt = null,
            long = 12345L,
            float = 67f,
            double = 89.0,
            arrayReferenceType = arrayReferenceType,
            nullableArrayReferenceType = null,
            arrayPrimitiveType = arrayPrimitiveType,
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(4, 6, 8).map { EvenInt(it) },
            nullableGenericCollectionType = null,
            genericType = EvenInt(2),
            nullableGenericType = null,
        )
        val b = ComplexPoko(
            referenceType = "Text",
            nullableReferenceType = null,
            int = 2,
            nullableInt = null,
            long = 12345L,
            float = 67f,
            double = 89.0,
            arrayReferenceType = arrayReferenceType,
            nullableArrayReferenceType = null,
            arrayPrimitiveType = arrayPrimitiveType,
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(4, 6, 8).map { EvenInt(it) },
            nullableGenericCollectionType = null,
            genericType = EvenInt(2),
            nullableGenericType = null,
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_inequivalent_compiled_Complex_instances_are_not_equals() {
        val arrayReferenceType = arrayOf("one string", "another string")
        val arrayPrimitiveType = intArrayOf(3, 4, 5)
        val a = ComplexPoko(
            referenceType = "Text",
            nullableReferenceType = null,
            int = 2,
            nullableInt = null,
            long = 12345L,
            float = 67f,
            double = 89.0,
            arrayReferenceType = arrayReferenceType,
            nullableArrayReferenceType = null,
            arrayPrimitiveType = arrayPrimitiveType,
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(4, 6, 8).map { EvenInt(it) },
            nullableGenericCollectionType = null,
            genericType = EvenInt(2),
            nullableGenericType = null,
        )
        val b = ComplexPoko(
            referenceType = "Text",
            nullableReferenceType = "non-null",
            int = 2,
            nullableInt = null,
            long = 12345L,
            float = 67f,
            double = 89.0,
            arrayReferenceType = arrayReferenceType,
            nullableArrayReferenceType = null,
            arrayPrimitiveType = arrayPrimitiveType,
            nullableArrayPrimitiveType = null,
            genericCollectionType = listOf(4, 6, 8).map { EvenInt(it) },
            nullableGenericCollectionType = null,
            genericType = EvenInt(2),
            nullableGenericType = null,
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    data class EvenInt(private val value: Int) : Number() {
        init { check(value % 2 == 0) }
        override fun toByte() = value.toByte()
        override fun toDouble() = value.toDouble()
        override fun toFloat() = value.toFloat()
        override fun toInt() = value
        override fun toLong() = value.toLong()
        override fun toShort() = value.toShort()
    }
}
