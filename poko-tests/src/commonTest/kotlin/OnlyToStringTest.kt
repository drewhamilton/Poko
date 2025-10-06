
import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import poko.OnlyToString

class OnlyToStringTest {
    @Test fun two_equivalent_compiled_OnlyToString_instances_are_not_equals() {
        val a = OnlyToString(name = "Only toString")
        val b = OnlyToString(name = "Only toString")
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
        assertThat(a).hashCodeFun().isNotEqualTo(b.hashCode())
    }

    @Test fun onlyToString_instance_has_expected_toString() {
        assertThat(OnlyToString("Title").toString()).isEqualTo("OnlyToString(name=Title)")
    }
}
