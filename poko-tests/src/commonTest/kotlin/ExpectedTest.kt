
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import poko.Expected

class ExpectedTest {
    @Test fun two_equivalent_compiled_Expected_instances_are_equals() {
        val a = Expected(1)
        val b = Expected(1)
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test fun two_inequivalent_compiled_Expected_instances_are_not_equals() {
        val a = Expected(2)
        val b = Expected(3)
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode())
    }

    @Test fun compiled_Expected_class_instance_has_expected_toString() {
        val actual = Expected(4)
        assertThat(actual.toString()).isEqualTo("Expected(value=4)")
    }
}
