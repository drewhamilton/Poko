import assertk.all
import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.toStringFun
import kotlin.test.Test
import poko.SuperclassWithFinalOverrides

class SuperclassWithFinalOverridesTest {

    @Test fun successful_instantiation_with_final_function_overrides_in_superclass() {
        val instance = SuperclassWithFinalOverrides.Subclass(name = "this-is-fine")
        assertThat(instance).all {
            toStringFun().isEqualTo("Subclass")
            hashCodeFun().isEqualTo(31 + "Subclass".hashCode())
            isEqualTo(SuperclassWithFinalOverrides.Subclass(name = "different-name"))
        }
    }
}
