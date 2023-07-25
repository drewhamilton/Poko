
import assertk.all
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.toStringFun
import kotlin.test.Test
import data.AnyArrayHolder as AnyArrayHolderData
import poko.AnyArrayHolder as AnyArrayHolderPoko

class AnyArrayHolderTest {
    @Test fun two_AnyArrayHolder_instances_holding_equivalent_typed_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = arrayOf("string A", "string B"),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = arrayOf("string A", "string B"),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_nested_typed_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = arrayOf(arrayOf("xx", "xy"), arrayOf("yx", "yy")),
            nullableAny = arrayOf(arrayOf(1L, 2f), arrayOf(3.0, 4)),
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = arrayOf(arrayOf("xx", "xy"), arrayOf("yx", "yy")),
            nullableAny = arrayOf(arrayOf(1L, 2f), arrayOf(3.0, 4)),
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_boolean_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = booleanArrayOf(false, true, true),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = booleanArrayOf(false, true, true),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_byte_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = byteArrayOf(Byte.MIN_VALUE, Byte.MAX_VALUE),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = byteArrayOf(Byte.MIN_VALUE, Byte.MAX_VALUE),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_char_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = charArrayOf('m', 'n', 'o', 'P'),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = charArrayOf('m', 'n', 'o', 'P'),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_short_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = shortArrayOf(6556.toShort()),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = shortArrayOf(6556.toShort()),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_int_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = intArrayOf(6556),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = intArrayOf(6556),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_long_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = longArrayOf(987654321L, 1234567890L),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = longArrayOf(987654321L, 1234567890L),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_float_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = floatArrayOf(1.2f, 2.3f, 3.4f),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = floatArrayOf(1.2f, 2.3f, 3.4f),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_double_arrays_match() {
        val a = AnyArrayHolderPoko(
            any = doubleArrayOf(0.0, -0.0, Double.NaN),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = doubleArrayOf(0.0, -0.0, Double.NaN),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_equivalent_nonarrays_match() {
        val a = AnyArrayHolderPoko(
            any = listOf("one", "two"),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = listOf("one", "two"),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)

            assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
            assertThat(a).toStringFun().isEqualTo(b.toString())
        }
    }

    @Test fun two_AnyArrayHolder_instances_holding_inequivalent_typed_arrays_are_not_equals() {
        val a = AnyArrayHolderPoko(
            any = arrayOf(arrayOf("xx", "xy"), arrayOf("yx", "yy")),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = arrayOf(arrayOf(1L, 2f), arrayOf(3.0, 4)),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun AnyArrayHolder_instances_holding_mismatching_types_are_not_equals() {
        val a = AnyArrayHolderPoko(
            any = arrayOf("x", "y"),
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        val b = AnyArrayHolderPoko(
            any = "xy",
            nullableAny = null,
            trailingProperty = "trailing string",
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun AnyArrayHolder_instances_holding_trailing_properties_are_not_equals() {
        val a = AnyArrayHolderPoko(
            any = arrayOf("string A", "string B"),
            nullableAny = null,
            trailingProperty = "1",
        )
        val b = AnyArrayHolderPoko(
            any = arrayOf("string A", "string B"),
            nullableAny = null,
            trailingProperty = "2",
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun AnyArrayHolder_has_same_behavior_as_handwritten_implementation() {
        val poko = AnyArrayHolderPoko(
            any = arrayOf(1, 2L, 3f, 4.0),
            nullableAny = arrayOf(1, 2L, 3f, 4.0),
            trailingProperty = "trailing",
        )
        val data = AnyArrayHolderData(
            any = arrayOf(1, 2L, 3f, 4.0),
            nullableAny = arrayOf(1, 2L, 3f, 4.0),
            trailingProperty = "trailing",
        )
        assertThat(poko).all {
            hashCodeFun().isEqualTo(data.hashCode())
            toStringFun().isEqualTo(data.toString())
        }
    }
}
