
import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.startsWith
import data.IdThing
import kotlin.test.Test
import poko.OnlyEqualsAndHashCode

class OnlyEqualsAndHashCodeTest {
    @Test fun two_equivalent_compiled_OnlyEqualsAndHashCode_instances_are_equals() {
        val a = OnlyEqualsAndHashCode(id = 1L)
        val b = OnlyEqualsAndHashCode(id = 1L)
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
        assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
    }

    @Test fun two_inequivalent_compiled_OnlyEqualsAndHashCode_instances_are_not_equals() {
        val a = OnlyEqualsAndHashCode(id = 2L)
        val b = OnlyEqualsAndHashCode(id = 3L)
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
        assertThat(a).hashCodeFun().isNotEqualTo(b.hashCode())
    }

    @Test fun onlyEqualsAndHashCode_instance_has_expected_hashCode() {
        val poko = OnlyEqualsAndHashCode(id = 4L)
        val data = IdThing(id = 4L)
        assertThat(poko).hashCodeFun().isEqualTo(data.hashCode())
    }

    @Test fun onlyEqualsAndHashCode_instance_has_generic_toString() {
        assertThat(OnlyEqualsAndHashCode(id = 100L).toString())
            .startsWith("poko.OnlyEqualsAndHashCode@")
    }
}
