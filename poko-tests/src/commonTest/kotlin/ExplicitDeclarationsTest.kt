import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import data.ExplicitDeclarations as ExplicitDeclarationsData
import poko.ExplicitDeclarations as ExplicitDeclarationsPoko

class ExplicitDeclarationsTest {
    @Test fun two_equivalent_compiled_ExplicitDeclarations_instances_are_equals() {
        val a = ExplicitDeclarationsPoko("string 1")
        val b = ExplicitDeclarationsPoko("string 2")
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun two_inequivalent_compiled_ExplicitDeclarations_instances_are_not_equals() {
        val a = ExplicitDeclarationsPoko("string 1")
        val b = ExplicitDeclarationsPoko("string 11")
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun compilation_with_explicit_function_declarations_respects_explicit_hashCode() {
        val testString = "test thing"
        val poko = ExplicitDeclarationsPoko(testString)
        val data = ExplicitDeclarationsData(testString)
        assertThat(poko.hashCode()).isEqualTo(testString.length)
        assertThat(poko.hashCode()).isEqualTo(data.hashCode())
    }

    @Test fun compilation_with_explicit_function_declarations_respects_explicit_toString() {
        val testString = "test string"
        val poko = ExplicitDeclarationsPoko(testString)
        val data = ExplicitDeclarationsData(testString)
        assertThat(poko.toString()).isEqualTo(testString)
        assertThat(poko.toString()).isEqualTo(data.toString())
    }
}
