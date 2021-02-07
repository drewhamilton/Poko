package dev.drewhamilton.poko.sample

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SampleTest {

    @Test fun `equals works`() {
        val a = Sample(
            int = 1,
            requiredString = "String",
            optionalString = null
        )
        val b = Sample(
            int = 1,
            requiredString = "String",
            optionalString = null
        )

        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun `hashCode is consistent`() {
        val a = Sample(
            int = 1,
            requiredString = "String",
            optionalString = null
        )
        val b = Sample(
            int = 1,
            requiredString = "String",
            optionalString = null
        )

        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test fun `hashCode is equivalent to data class hashCode`() {
        val dataApi = Sample(
            int = 1,
            requiredString = "String",
            optionalString = null
        )

        val data = DataSample(
            int = 1,
            requiredString = "String",
            optionalString = null
        )

        assertThat(dataApi.hashCode()).isEqualTo(data.hashCode())
    }

    @Test fun `toString includes class name and each property`() {
        val sample = Sample(3, "sample", null)
        assertThat(sample.toString()).isEqualTo("Sample(int=3, requiredString=sample, optionalString=null)")
    }

    @Test fun `toString is equivalent to data class toString`() {
        val dataApi = Sample(
            int = 99,
            requiredString = "test",
            optionalString = null
        )

        val data = DataSample(
            int = 99,
            requiredString = "test",
            optionalString = null
        )

        assertThat(dataApi.toString()).isEqualTo(data.toString().removePrefix("Data"))
    }

    /**
     * Data class equivalent to [Sample].
     */
    private data class DataSample(
        val int: Int,
        val requiredString: String,
        val optionalString: String?
    )
}
