package dev.drewhamilton.poko.sample.jvm

import assertk.assertThat
import assertk.assertions.isNull
import org.junit.Test

class SampleBuilderTest {
    @Test fun `instantiate builder`() {
        val builder = Sample.Builder()
        assertThat<Int?>(builder.int).isNull()
        assertThat<String?>(builder.requiredString).isNull()
        assertThat<String?>(builder.optionalString).isNull()
    }
}
