package dev.drewhamilton.careful.sample

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SimpleTest {

    @Test fun `toString includes class name and each property`() {
        val sample = Simple(3, "sample", null)
        assertThat(sample.toString()).isEqualTo("Sample(int=3, requiredString=sample, optionalString=null)")
    }
}
