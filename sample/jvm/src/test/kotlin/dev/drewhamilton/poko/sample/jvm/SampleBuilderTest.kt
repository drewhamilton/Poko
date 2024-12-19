package dev.drewhamilton.poko.sample.jvm

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.message
import org.junit.Test

class SampleBuilderTest {
    @Test fun `setters behave as expected`() {
        val builder = Sample.Builder()
        assertThat<Int?>(builder.int).isNull()
        assertThat<String?>(builder.requiredString).isNull()
        assertThat<String?>(builder.optionalString).isNull()

        builder
            .setInt(1)
            .setRequiredString("two")
            .setOptionalString(null)
            .setOptionalString("three")

        assertThat<Int?>(builder.int).isEqualTo(1)
        assertThat<String?>(builder.requiredString).isEqualTo("two")
        assertThat<String?>(builder.optionalString).isEqualTo("three")

        builder.apply {
            int = 4
            requiredString = "five"
            optionalString = null
        }

        assertThat<Int?>(builder.int).isEqualTo(4)
        assertThat<String?>(builder.requiredString).isEqualTo("five")
        assertThat<String?>(builder.optionalString).isNull()
    }

    @Test fun `valid build succeeds`() {
        val sample: Sample = Sample.Builder()
            .setInt(1)
            .setRequiredString("required")
            .build()

        assertThat(sample).isEqualTo(
            expected = Sample(
                int = 1,
                requiredString = "required",
                optionalString = null,
            )
        )
    }

    @Test fun `invalid build fails`() {
        // Missing int:
        assertFailure {
            Sample.Builder()
                .setRequiredString("req")
                .build()
        }
            .message()
            .isNotNull()
            .contains("is null")

        // Missing string:
        assertFailure {
            Sample.Builder()
                .setInt(66)
                .build()
        }
            .message()
            .isNotNull()
            .contains("is null")
    }
}
