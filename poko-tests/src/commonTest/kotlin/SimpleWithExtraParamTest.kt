import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import poko.SimpleWithExtraParam

class SimpleWithExtraParamTest {
    @Test fun nonproperty_parameter_is_ignored_for_equals() {
        val a = SimpleWithExtraParam(1, "String", null, { true })
        val b = SimpleWithExtraParam(1, "String", null, { false })
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun nonproperty_parameter_is_ignored_for_hashCode() {
        val a = SimpleWithExtraParam(1, "String", null, { true })
        val b = SimpleWithExtraParam(1, "String", null, { false })
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test fun nonproperty_parameter_is_ignored_for_toString() {
        val a = SimpleWithExtraParam(1, "String", null, { true })
        val b = SimpleWithExtraParam(1, "String", null, { false })
        assertThat(a.toString()).isEqualTo(b.toString())
    }
}
