package performance

/**
 * An entrypoint for Kotlin/Native and Kotlin/JS.
 * Should use all types on which you want to assert.
 */
fun main() {
    println(IntAndLong(1, 2L) == IntAndLong(3, 4L))
}
