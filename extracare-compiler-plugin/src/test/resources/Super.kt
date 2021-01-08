import kotlin.random.Random

abstract class Super {
    override fun equals(other: Any?): Boolean = Random.nextBoolean()
    override fun hashCode(): Int = 50934
    override fun toString(): String = "superclass"
}
