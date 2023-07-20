import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import data.SuperclassDeclarations as SuperclassDeclarationsData
import poko.SuperclassDeclarations as SuperclassDeclarationsPoko

class SuperclassDeclarationsTest {
    @Test fun two_equivalent_compiled_Subclass_instances_are_equals() {
        val a = SuperclassDeclarationsPoko(999.9)
        val b = SuperclassDeclarationsPoko(999.9)

        // Super class equals implementation returns `other == true`; this confirms that is overridden:
        assertThat(a).isNotEqualTo(true)
        assertThat(b).isNotEqualTo(true)

        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun two_inequivalent_compiled_Subclass_instances_are_not_equals() {
        val a = SuperclassDeclarationsPoko(999.9)
        val b = SuperclassDeclarationsPoko(888.8)
        // Super class equals implementation returns `other == true`; this confirms that is overridden:
        assertThat(a).isNotEqualTo(true)
        assertThat(b).isNotEqualTo(true)

        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun superclass_hashCode_is_overridden() {
        val poko = SuperclassDeclarationsPoko(123.4)
        val data = SuperclassDeclarationsData(123.4)
        assertThat(poko.hashCode()).isEqualTo(data.hashCode())
        assertThat(poko.hashCode()).isNotEqualTo(50934)
    }

    @Test fun superclass_toString_is_overridden() {
        val poko = SuperclassDeclarationsPoko(123.4)
        val data = SuperclassDeclarationsData(123.4)
        assertThat(poko.toString()).isEqualTo(data.toString())
        assertThat(poko.toString()).isNotEqualTo("superclass")
    }
}
