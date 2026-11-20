package api

import dev.drewhamilton.poko.Poko

interface DataInterface {
    val id: String

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

@Poko class MyData(
    override val id: String,
) : DataInterface
