package dev.drewhamilton.poko.sample.mpp

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test

class RedactSampleTest {

    @Test fun toString_masks_redacted_properties() {
        val sample = RedactSample(id = "a", token = "secret-token", ssn = "123-45-6789")

        assertThat(sample.toString()).isEqualTo("RedactSample(id=a, token=***, ssn=<redacted>)")
    }

    @Test fun equals_uses_redacted_properties() {
        val a = RedactSample(id = "a", token = "one", ssn = null)
        val b = RedactSample(id = "a", token = "two", ssn = null)

        assertThat(a).isNotEqualTo(b)
    }
}
