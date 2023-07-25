import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.toStringFun
import kotlin.test.Test
import poko.SimpleWithExtraParam

class SimpleWithExtraParamTest {
    @Test fun nonproperty_parameter_is_ignored_for_equals() {
        val a = SimpleWithExtraParam(1, "String", null, { true })
        val b = SimpleWithExtraParam(1, "String", null, { false })
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
        assertThat(a).hashCodeFun().isEqualTo(b.hashCode())
        assertThat(a).toStringFun().isEqualTo(b.toString())
    }
}
