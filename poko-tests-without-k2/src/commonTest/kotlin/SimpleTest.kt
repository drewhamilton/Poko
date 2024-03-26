import assertk.all
import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.toStringFun
import kotlin.test.Test
import data.Simple as SimpleData
import poko.Simple as SimplePoko

class SimpleTest {
    @Test fun two_equivalent_compiled_Simple_instances_are_equals() {
        val a = SimplePoko(1, "String", null)
        val b = SimplePoko(1, "String", null)
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun two_inequivalent_compiled_Simple_instances_are_not_equals() {
        val a = SimplePoko(1, "String", null)
        val b = SimplePoko(1, "String", "non-null")
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun compiled_Simple_class_instance_has_expected_hashCode() {
        val poko = SimplePoko(1, "String", null)
        val data = SimpleData(1, "String", null)
        assertThat(poko).all {
            hashCodeFun().isEqualTo(data.hashCode())
            toStringFun().isEqualTo(data.toString())
        }
    }
}
