package dev.drewhamilton.poko.sample.jvm

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

class SampleBuilderTest {
    @Test fun `instantiate builder`() {
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
    }
}
