package dev.drewhamilton.careful.sample

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SimpleTest {

    @Test fun `equals works`() {
        val a = Simple(
            int = 1,
            requiredString = "String",
            optionalString = null
        )
        val b = Simple(
            int = 1,
            requiredString = "String",
            optionalString = null
        )

        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun `toString includes class name and each property`() {
        val simple = Simple(3, "sample", null)
        assertThat(simple.toString()).isEqualTo("Simple(int=3, requiredString=sample, optionalString=null)")
    }
}
