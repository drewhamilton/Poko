package dev.drewhamilton.poko.sample.mpp

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class SampleTest {

    @Test fun equals_works() {
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

    @Test fun hashCode_is_consistent() {
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

    @Test fun hashCode_is_equivalent_to_data_class_hashCode() {
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

    @Test fun toString_includes_class_name_and_each_property() {
        val sample = Sample(3, "sample", null)
        assertThat(sample.toString())
            .isEqualTo("Sample(int=3, requiredString=sample, optionalString=null)")
    }

    @Test fun toString_is_equivalent_to_data_class_toString() {
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
