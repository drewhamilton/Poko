
import assertk.all
import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.toStringFun
import kotlin.test.Test
import data.MyData as MyDataData
import poko.MyData as MyDataPoko

class DataInterfaceTest {
    @Test fun two_equivalent_compiled_MyData_instances_are_equals() {
        val a = MyDataPoko("id")
        val b = MyDataPoko("id")
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun two_inequivalent_compiled_MyData_instances_are_not_equals() {
        val a = MyDataPoko(id = "a")
        val b = MyDataPoko(id = "b")
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun compiled_MyData_class_instance_has_expected_hashCode() {
        val poko = MyDataPoko("id")
        val data = MyDataData("id")
        assertThat(poko).all {
            hashCodeFun().isEqualTo(data.hashCode())
            toStringFun().isEqualTo(data.toString())
        }
    }
}
