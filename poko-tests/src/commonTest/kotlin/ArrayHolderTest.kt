import assertk.assertAll
import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.toStringFun
import kotlin.test.Test
import poko.ArrayHolder

class ArrayHolderTest {
    @Test fun two_equivalent_compiled_ArrayHolder_instances_match() {
        val a = ArrayHolder(
            stringArray = arrayOf("one string", "another string"),
            nullableStringArray = null,
            booleanArray = booleanArrayOf(true, false),
            nullableBooleanArray = null,
            byteArray = byteArrayOf(1.toByte(), 2.toByte()),
            nullableByteArray = null,
            charArray = charArrayOf('a', 'b', 'c', 'c'),
            nullableCharArray = null,
            shortArray = shortArrayOf(99.toShort(), 88.toShort()),
            nullableShortArray = null,
            intArray = intArrayOf(3, 4, 5),
            nullableIntArray = null,
            longArray = longArrayOf(98765L, 43210L),
            nullableLongArray = null,
            floatArray = floatArrayOf(3.14f, 1519f),
            nullableFloatArray = null,
            doubleArray = doubleArrayOf(2.22222, Double.NaN),
            nullableDoubleArray = null,
            nestedStringArray = arrayOf(
                arrayOf("1A", "2A"),
                arrayOf("1B", "2B", "3B"),
            ),
            nestedIntArray = arrayOf(
                intArrayOf(1, 2, 3, 4),
                intArrayOf(99, 98, 97),
            ),
        )
        val b = ArrayHolder(
            stringArray = arrayOf("one string", "another string"),
            nullableStringArray = null,
            booleanArray = booleanArrayOf(true, false),
            nullableBooleanArray = null,
            byteArray = byteArrayOf(1.toByte(), 2.toByte()),
            nullableByteArray = null,
            charArray = charArrayOf('a', 'b', 'c', 'c'),
            nullableCharArray = null,
            shortArray = shortArrayOf(99.toShort(), 88.toShort()),
            nullableShortArray = null,
            intArray = intArrayOf(3, 4, 5),
            nullableIntArray = null,
            longArray = longArrayOf(98765L, 43210L),
            nullableLongArray = null,
            floatArray = floatArrayOf(3.14f, 1519f),
            nullableFloatArray = null,
            doubleArray = doubleArrayOf(2.22222, Double.NaN),
            nullableDoubleArray = null,
            nestedStringArray = arrayOf(
                arrayOf("1A", "2A"),
                arrayOf("1B", "2B", "3B"),
            ),
            nestedIntArray = arrayOf(
                intArrayOf(1, 2, 3, 4),
                intArrayOf(99, 98, 97),
            ),
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_inequivalent_compiled_ArrayHolder_instances_are_not_equals() {
        val a = ArrayHolder(
            stringArray = arrayOf("one string", "another string"),
            nullableStringArray = null,
            booleanArray = booleanArrayOf(true, false),
            nullableBooleanArray = null,
            byteArray = byteArrayOf(1.toByte(), 2.toByte()),
            nullableByteArray = null,
            charArray = charArrayOf('a', 'b', 'c', 'c'),
            nullableCharArray = null,
            shortArray = shortArrayOf(99.toShort(), 88.toShort()),
            nullableShortArray = null,
            intArray = intArrayOf(3, 4, 5),
            nullableIntArray = null,
            longArray = longArrayOf(98765L, 43210L),
            nullableLongArray = null,
            floatArray = floatArrayOf(3.14f, 1519f),
            nullableFloatArray = null,
            doubleArray = doubleArrayOf(2.22222, Double.NaN),
            nullableDoubleArray = null,
            nestedStringArray = arrayOf(
                arrayOf("1A", "2A"),
                arrayOf("1B", "2B", "3B"),
            ),
            nestedIntArray = arrayOf(
                intArrayOf(1, 2, 3, 4),
                intArrayOf(99, 98, 97),
            ),
        )
        val b = ArrayHolder(
            stringArray = arrayOf("just one string"), // <-- Different from a
            nullableStringArray = null,
            booleanArray = booleanArrayOf(true, false),
            nullableBooleanArray = null,
            byteArray = byteArrayOf(1.toByte(), 2.toByte()),
            nullableByteArray = null,
            charArray = charArrayOf('a', 'b', 'c', 'c'),
            nullableCharArray = null,
            shortArray = shortArrayOf(99.toShort(), 88.toShort()),
            nullableShortArray = null,
            intArray = intArrayOf(3, 4, 5),
            nullableIntArray = null,
            longArray = longArrayOf(98765L, 43210L),
            nullableLongArray = null,
            floatArray = floatArrayOf(3.14f, 1519f),
            nullableFloatArray = null,
            doubleArray = doubleArrayOf(2.22222, Double.NaN),
            nullableDoubleArray = null,
            nestedStringArray = arrayOf(
                arrayOf("1A", "2A"),
                arrayOf("1B", "2B", "3B"),
            ),
            nestedIntArray = arrayOf(
                intArrayOf(1, 2, 3, 4),
                intArrayOf(99, 98, 97),
            ),
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)

        val c = ArrayHolder(
            stringArray = arrayOf("one string", "another string"),
            nullableStringArray = null,
            booleanArray = booleanArrayOf(true, false),
            nullableBooleanArray = null,
            byteArray = byteArrayOf(1.toByte(), 2.toByte()),
            nullableByteArray = null,
            charArray = charArrayOf('x', 'y', 'z'), // <-- Different from a
            nullableCharArray = null,
            shortArray = shortArrayOf(99.toShort(), 88.toShort()),
            nullableShortArray = null,
            intArray = intArrayOf(3, 4, 5),
            nullableIntArray = null,
            longArray = longArrayOf(98765L, 43210L),
            nullableLongArray = null,
            floatArray = floatArrayOf(3.14f, 1519f),
            nullableFloatArray = null,
            doubleArray = doubleArrayOf(2.22222, Double.NaN),
            nullableDoubleArray = null,
            nestedStringArray = arrayOf(
                arrayOf("1A", "2A"),
                arrayOf("1B", "2B", "3B"),
            ),
            nestedIntArray = arrayOf(
                intArrayOf(1, 2, 3, 4),
                intArrayOf(99, 98, 97),
            ),
        )
        assertThat(a).isNotEqualTo(c)
        assertThat(c).isNotEqualTo(a)
    }
}
