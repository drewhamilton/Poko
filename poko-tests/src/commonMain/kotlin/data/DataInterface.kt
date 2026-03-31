package data

interface DataInterface {
    val id: String

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

data class MyData(
    override val id: String,
) : DataInterface
