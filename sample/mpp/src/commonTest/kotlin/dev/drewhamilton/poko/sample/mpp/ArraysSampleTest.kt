package dev.drewhamilton.poko.sample.mpp

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ArraysSampleTest {

    @Test fun equals_works() {
        val a = ArraysSample(
            primitive = byteArrayOf(0x1F.toByte()),
            standard = arrayOf("string 1", "string 2"),
            nested = arrayOf(charArrayOf('a'), charArrayOf('b')),
            runtime = arrayOf<Any>("one", 2),
        )
        val b = ArraysSample(
            primitive = byteArrayOf(0x1F.toByte()),
            standard = arrayOf("string 1", "string 2"),
            nested = arrayOf(charArrayOf('a'), charArrayOf('b')),
            runtime = arrayOf<Any>("one", 2),
        )

        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
    }

    @Test fun hashCode_is_consistent() {
        val a = ArraysSample(
            primitive = byteArrayOf(0x1F.toByte()),
            standard = arrayOf("string 1", "string 2"),
            nested = arrayOf(charArrayOf('a'), charArrayOf('b')),
            runtime = arrayOf<Any>("one", 2),
        )
        val b = ArraysSample(
            primitive = byteArrayOf(0x1F.toByte()),
            standard = arrayOf("string 1", "string 2"),
            nested = arrayOf(charArrayOf('a'), charArrayOf('b')),
            runtime = arrayOf<Any>("one", 2),
        )

        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test fun toString_includes_class_name_and_each_property() {
        val sample = ArraysSample(
            primitive = byteArrayOf(0x1F.toByte()),
            standard = arrayOf("string 1", "string 2"),
            nested = arrayOf(charArrayOf('a'), charArrayOf('b')),
            runtime = arrayOf<Any>("one", 2),
        )
        assertThat(sample.toString()).isEqualTo(
            other = "ArraysSample(" +
                "primitive=[31], " +
                "standard=[string 1, string 2], " +
                "nested=[[a], [b]], " +
                "runtime=[one, 2])",
        )
    }
}
