package data

/**
 * Data classes don't implement array content checks, so [equals], [hashCode], and [toString] must
 * be written by hand.
 */
@Suppress("Unused")
data class AnyArrayHolder(
    val any: Any,
    val nullableAny: Any?,
    val trailingProperty: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true

        if (other !is AnyArrayHolder)
            return false

        if (!this.any.arrayContentDeepEquals(other.any))
            return false

        if (!this.nullableAny.arrayContentDeepEquals(other.nullableAny))
            return false

        if (this.trailingProperty != other.trailingProperty)
            return false

        return true
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Any?.arrayContentDeepEquals(other: Any?): Boolean {
        return when (this) {
            is Array<*> -> other is Array<*> && this.contentDeepEquals(other)
            is BooleanArray -> other is BooleanArray && this.contentEquals(other)
            is ByteArray -> other is ByteArray && this.contentEquals(other)
            is CharArray -> other is CharArray && this.contentEquals(other)
            is ShortArray -> other is ShortArray && this.contentEquals(other)
            is IntArray -> other is IntArray && this.contentEquals(other)
            is LongArray -> other is LongArray && this.contentEquals(other)
            is FloatArray -> other is FloatArray && this.contentEquals(other)
            is DoubleArray -> other is DoubleArray && this.contentEquals(other)
            else -> this == other
        }
    }

    override fun hashCode(): Int {
        var result = any.arrayContentDeepHashCode()

        result = result * 31 + nullableAny.arrayContentDeepHashCode()
        result = result * 31 + trailingProperty.hashCode()

        return result
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Any?.arrayContentDeepHashCode(): Int {
        return when (this) {
            is Array<*> -> this.contentDeepHashCode()
            is BooleanArray -> this.contentHashCode()
            is ByteArray -> this.contentHashCode()
            is CharArray -> this.contentHashCode()
            is ShortArray -> this.contentHashCode()
            is IntArray -> this.contentHashCode()
            is LongArray -> this.contentHashCode()
            is FloatArray -> this.contentHashCode()
            is DoubleArray -> this.contentHashCode()
            else -> this.hashCode()
        }
    }

    override fun toString(): String {
        return StringBuilder()
            .append("AnyArrayHolder(")
            .append("any=")
            .append(any.arrayContentDeepToString())
            .append(", ")
            .append("nullableAny=")
            .append(nullableAny.arrayContentDeepToString())
            .append(", ")
            .append("trailingProperty=")
            .append(trailingProperty)
            .append(")")
            .toString()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Any?.arrayContentDeepToString(): String {
        return when (this) {
            is Array<*> -> this.contentDeepToString()
            is BooleanArray -> this.contentToString()
            is ByteArray -> this.contentToString()
            is CharArray -> this.contentToString()
            is ShortArray -> this.contentToString()
            is IntArray -> this.contentToString()
            is LongArray -> this.contentToString()
            is FloatArray -> this.contentToString()
            is DoubleArray -> this.contentToString()
            else -> this.toString()
        }
    }
}
