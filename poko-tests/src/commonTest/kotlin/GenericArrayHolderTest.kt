import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.toStringFun
import kotlin.test.Test
import poko.GenericArrayHolder

class GenericArrayHolderTest {
    @Suppress("RemoveExplicitTypeArguments") // https://youtrack.jetbrains.com/issue/KT-61627
    @Test fun two_GenericArrayHolder_instances_with_equivalent_typed_arrays_are_equals() {
        val a = GenericArrayHolder(arrayOf<Any>(arrayOf("5%, 10%"), intArrayOf(5, 10), booleanArrayOf(false, true)))
        val b = GenericArrayHolder(arrayOf<Any>(arrayOf("5%, 10%"), intArrayOf(5, 10), booleanArrayOf(false, true)))
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
        assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
        assertThat(a).toStringFun().isEqualTo(b.toString())
    }

    @Test fun two_GenericArrayHolder_instances_with_equivalent_int_arrays_are_equals() {
        val a = GenericArrayHolder(intArrayOf(5, 10))
        val b = GenericArrayHolder(intArrayOf(5, 10))
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
        assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
        assertThat(a).toStringFun().isEqualTo(b.toString())
    }

    @Test fun two_GenericArrayHolder_instances_with_equivalent_nonarrays_are_equals() {
        val a = GenericArrayHolder("5, 10")
        val b = GenericArrayHolder("5, 10")
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
        assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
        assertThat(a).toStringFun().isEqualTo(b.toString())
    }

    @Test fun two_GenericArrayHolder_instances_holding_inequivalent_long_arrays_are_not_equals() {
        val a = GenericArrayHolder(longArrayOf(Long.MIN_VALUE))
        val b = GenericArrayHolder(longArrayOf(Long.MAX_VALUE))
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun two_GenericArrayHolder_instances_holding_mismatching_types_are_not_equals() {
        val a = GenericArrayHolder(arrayOf("x", "y"))
        val b = GenericArrayHolder("xy")
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }
}
