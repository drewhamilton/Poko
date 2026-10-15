import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import poko.OnlyEqualsAndHashCodeRedacted
import poko.OnlyToStringRedacted
import poko.RedactedFirstProperty
import poko.RedactedProperty
import poko.SkippedRedactedProperty

class RedactedPropertyTest {

    @Test fun redacted_values_are_masked_in_toString() {
        assertThat(redacted().toString())
            .isEqualTo("RedactedProperty(id=a, token=***, ssn=<redacted>, bytes=***, note=)")
    }

    @Test fun null_redacted_value_prints_mask() {
        assertThat(redacted(ssn = null).toString())
            .isEqualTo("RedactedProperty(id=a, token=***, ssn=<redacted>, bytes=***, note=)")
    }

    @Test fun redacted_first_property_is_followed_by_separator() {
        assertThat(RedactedFirstProperty(secret = "s", id = "a").toString())
            .isEqualTo("RedactedFirstProperty(secret=***, id=a)")
    }

    @Test fun redacted_properties_participate_in_equals() {
        val base = redacted()
        assertAll {
            assertThat(redacted(token = "other")).isNotEqualTo(base)
            assertThat(redacted(ssn = "other")).isNotEqualTo(base)
            assertThat(redacted(ssn = null)).isNotEqualTo(base)
            assertThat(redacted(bytes = byteArrayOf(9))).isNotEqualTo(base)
            assertThat(redacted(note = "other")).isNotEqualTo(base)
        }
    }

    @Test fun redacted_property_participates_in_hashCode() {
        assertThat(redacted(token = "x").hashCode())
            .isNotEqualTo(redacted(token = "y").hashCode())
    }

    @Test fun equal_redacted_values_are_equal_with_same_hashCode() {
        val a = redacted(bytes = byteArrayOf(1, 2, 3))
        val b = redacted(bytes = byteArrayOf(1, 2, 3))
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)
            assertThat(a.hashCode()).isEqualTo(b.hashCode())
        }
    }

    @Test fun skip_wins_over_redact() {
        val a = SkippedRedactedProperty(id = "id", secret = "one")
        val b = SkippedRedactedProperty(id = "id", secret = "two")
        assertAll {
            assertThat(a).isEqualTo(b)
            assertThat(a.hashCode()).isEqualTo(b.hashCode())
            assertThat(a.toString()).isEqualTo("SkippedRedactedProperty(id=id)")
        }
    }

    @Test fun toString_only_class_masks_redacted_property() {
        assertThat(OnlyToStringRedacted(name = "n", secret = "s").toString())
            .isEqualTo("OnlyToStringRedacted(name=n, secret=***)")
    }

    @Test fun equalsAndHashCode_only_class_compares_redacted_property() {
        assertAll {
            assertThat(OnlyEqualsAndHashCodeRedacted(id = 1, secret = "s"))
                .isEqualTo(OnlyEqualsAndHashCodeRedacted(id = 1, secret = "s"))
            assertThat(OnlyEqualsAndHashCodeRedacted(id = 1, secret = "s"))
                .isNotEqualTo(OnlyEqualsAndHashCodeRedacted(id = 1, secret = "t"))
        }
    }

    private fun redacted(
        id: String = "a",
        token: String = "secret-token",
        ssn: String? = "123-45-6789",
        bytes: ByteArray = byteArrayOf(1, 2, 3),
        note: String = "private note",
    ) = RedactedProperty(id = id, token = token, ssn = ssn, bytes = bytes, note = note)
}
