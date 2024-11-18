
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import poko.SkippedProperty

class SkippedPropertyTest {

    @Test fun skipped_property_omitted_from_all_generated_functions() {
        val a = poko.SkippedProperty(
            poko.SkippedProperty.id = "id",
            callback = { println("Callback <a> invoked") },
        )
        val b = SkippedProperty(
            id = "id",
            callback = { println("Callback <b> invoked") },
        )

        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)
            assertThat(a.hashCode()).isEqualTo(b.hashCode())
            assertThat(a.toString()).isEqualTo(b.toString())
            assertThat(a.toString()).isEqualTo("SkippedProperty(id=id)")
        }
    }
}