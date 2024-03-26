import assertk.assertThat
import assertk.assertions.isEqualTo
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
}
