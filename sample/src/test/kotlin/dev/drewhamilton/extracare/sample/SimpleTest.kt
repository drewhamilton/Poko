package dev.drewhamilton.extracare.sample

import com.google.common.truth.Truth.assertThat
import dev.drewhamilton.extracare.sample.alt.DataSimple
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

    @Test fun `hashCode is consistent`() {
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

        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test fun `hashCode is equivalent to data class hashCode`() {
        val dataApi = Simple(
            int = 1,
            requiredString = "String",
            optionalString = null
        )

        val data = DataSimple(
            int = 1,
            requiredString = "String",
            optionalString = null
        )

        assertThat(dataApi.hashCode()).isEqualTo(data.hashCode())
    }

    @Test fun `toString includes class name and each property`() {
        val simple = Simple(3, "sample", null)
        assertThat(simple.toString()).isEqualTo("Simple(int=3, requiredString=sample, optionalString=null)")
    }

    @Test fun `toString is equivalent to data class toString`() {
        val dataApi = Simple(
            int = 99,
            requiredString = "test",
            optionalString = null
        )

        val data = DataSimple(
            int = 99,
            requiredString = "test",
            optionalString = null
        )

        assertThat(dataApi.toString()).isEqualTo(data.toString().removePrefix("Data"))
    }
}
