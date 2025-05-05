import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.toStringFun
import kotlin.test.Test
import poko.ComplexGenericArrayHolder

class ComplexGenericArrayHolderTest {
    @Test fun two_ComplexGenericArrayHolder_instances_with_equivalent_int_arrays_are_equals() {
        val a = ComplexGenericArrayHolder(
            generic = intArrayOf(50, 100),
        )
        val b = ComplexGenericArrayHolder(
            generic = intArrayOf(50, 100),
        )
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun hashCode_produces_expected_value() {
        val value = ComplexGenericArrayHolder(
            generic = intArrayOf(50, 100),
        )
        // Ensure consistency across platforms:
        assertThat(value).hashCodeFun().isEqualTo(2611)
    }

    @Test fun toString_produces_expected_value() {
        val value = ComplexGenericArrayHolder(
            generic = intArrayOf(50, 100),
        )
        assertThat(value).toStringFun().isEqualTo("ComplexGenericArrayHolder(generic=[50, 100])")
    }
}
