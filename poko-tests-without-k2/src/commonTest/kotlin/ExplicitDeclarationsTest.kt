import assertk.all
import assertk.assertThat
import assertk.assertions.hashCodeFun
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.toStringFun
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
        val testString = "test string"
        val poko = ExplicitDeclarationsPoko(testString)
        val data = ExplicitDeclarationsData(testString)

        assertThat(poko).all {
            hashCodeFun().all {
                isEqualTo(testString.length)
                isEqualTo(data.hashCode())
            }
            toStringFun().all {
                isEqualTo(testString)
                isEqualTo(data.toString())
            }
        }
    }
}
