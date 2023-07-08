package dev.drewhamilton.poko.sample.jvm.arrays

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.matches
import org.junit.Test

class ArrayTest {

    //region equals
    @Test fun `data equals does not work`() {
        val a = DataArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = null,
        )
        val b = DataArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = null,
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun `handwritten equals does not work`() {
        val a = HandwrittenArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = null,
        )
        val b = HandwrittenArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = null,
        )
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
    }

    @Test fun `poko equals does not work`() {
        val a = PokoArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = null,
        )
        val b = PokoArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = null,
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
            maybe = null,
        )
        val handwritten = HandwrittenArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = null,
        )
        val poko = PokoArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = null,
        )

        assertThat(data.hashCode()).isEqualTo(handwritten.hashCode())
        assertThat(poko.hashCode()).isEqualTo(handwritten.hashCode())
    }

    @Test fun `hidden array breaks hashCode`() {
        val data = DataArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = arrayOf("3", "4"),
        )
        val handwritten = HandwrittenArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = arrayOf("3", "4"),
        )
        val poko = PokoArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = arrayOf("3", "4"),
        )

        assertThat(data.hashCode()).isNotEqualTo(handwritten.hashCode())
        assertThat(poko.hashCode()).isNotEqualTo(handwritten.hashCode())
        assertThat(poko.hashCode()).isNotEqualTo(data.hashCode())
    }
    //endregion

    //region toString
    @Test fun `data toString is as expected`() {
        val a = DataArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = arrayOf("3", "4"),
        )


        val expected = Regex(
            "DataArrayHolder\\(id=id, array=\\[one, two], maybe=\\[Ljava.lang.String;@[0-9a-fA-F]+\\)"
        )
        assertThat(a.toString()).matches(expected)
    }

    @Test fun `handwritten toString is as expected`() {
        val a = HandwrittenArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = arrayOf("3", "4"),
        )

        val expected = Regex(
            "HandwrittenArrayHolder\\(id=id, array=\\[one, two], maybe=\\[Ljava.lang.String;@[0-9a-fA-F]+\\)"
        )
        assertThat(a.toString()).matches(expected)
    }

    @Test fun `poko toString is as expected`() {
        val a = PokoArrayHolder(
            id = "id",
            array = arrayOf("one", "two"),
            maybe = arrayOf("3", "4"),
        )

        val expected = Regex(
            "PokoArrayHolder\\(id=id, array=\\[one, two], maybe=\\[Ljava.lang.String;@[0-9a-fA-F]+\\)"
        )
        assertThat(a.toString()).matches(expected)
    }
    //endregion
}
