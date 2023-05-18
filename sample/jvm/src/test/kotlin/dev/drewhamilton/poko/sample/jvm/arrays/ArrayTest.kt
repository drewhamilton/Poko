package dev.drewhamilton.poko.sample.jvm.arrays

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArrayTest {

    //region equals
    @Test fun `data equals does not work`() {
        val a = DataArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )
        val b = DataArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun `handwritten equals does not work`() {
        val a = HandwrittenArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )
        val b = HandwrittenArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun `poko equals does not work`() {
        val a = PokoArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )
        val b = PokoArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }
    //endregion

    //region hashCode
    @Test fun `hashCodes are all equal`() {
        val data = DataArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )
        val handwritten = HandwrittenArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )
        val poko = PokoArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )

        assertThat(data.hashCode()).isEqualTo(handwritten.hashCode())
        assertThat(poko.hashCode()).isEqualTo(handwritten.hashCode())
    }
    //endregion

    //region toString
    @Test fun `data toString is as expected`() {
        val a = DataArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )

        val expected = "DataArrayHolder(id=id, array=[one, two])"
        assertThat(a.toString()).isEqualTo(expected)
    }

    @Test fun `handwritten toString is as expected`() {
        val a = HandwrittenArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )

        val expected = "HandwrittenArrayHolder(id=id, array=[one, two])"
        assertThat(a.toString()).isEqualTo(expected)
    }

    @Test fun `poko toString is as expected`() {
        val a = PokoArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
        )

        val expected = "PokoArrayHolder(id=id, array=[one, two])"
        assertThat(a.toString()).isEqualTo(expected)
    }
    //endregion
}
