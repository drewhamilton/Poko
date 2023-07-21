import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import data.OuterClass.Nested as NestedData
import poko.OuterClass.Nested as NestedPoko

class NestedTest {
    @Test fun two_equivalent_compiled_Nested_instances_are_equals() {
        val a = NestedPoko("string 1")
        val b = NestedPoko("string 1")
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun two_inequivalent_compiled_Nested_instances_are_not_equals() {
        val a = NestedPoko("string 1")
        val b = NestedPoko("string 2")
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun compilation_of_nested_class_within_class_matches_corresponding_data_class_hashCode() {
        val poko = NestedPoko("nested class value")
        val data = NestedData("nested class value")
        assertThat(poko.hashCode()).isEqualTo(data.hashCode())
    }

    @Test fun compilation_of_nested_class_within_class_matches_corresponding_data_class_toString() {
        val poko = NestedPoko("nested class value")
        val data = NestedData("nested class value")
        assertThat(poko.toString()).isEqualTo(data.toString())
    }
}
