package data

/**
 * Data classes don't implement array content checks, so [equals], [hashCode], and [toString] must
 * be written by hand.
 */
@Suppress("Unused")
data class AnyArrayHolder(
    val any: Any,
    val nullableAny: Any?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true

        if (other !is AnyArrayHolder)
            return false

        when (this.any) {
            is Array<*> ->
                if (other.any !is Array<*> || !this.any.contentDeepEquals(other.any))
                    return false
            is BooleanArray ->
                if (other.any !is BooleanArray || !this.any.contentEquals(other.any))
                    return false
            is ByteArray ->
                if (other.any !is ByteArray || !this.any.contentEquals(other.any))
                    return false
            is CharArray ->
                if (other.any !is CharArray || !this.any.contentEquals(other.any))
                    return false
            is ShortArray ->
                if (other.any !is ShortArray || !this.any.contentEquals(other.any))
                    return false
            is IntArray ->
                if (other.any !is IntArray || !this.any.contentEquals(other.any))
                    return false
            is LongArray ->
                if (other.any !is LongArray || !this.any.contentEquals(other.any))
                    return false
            is FloatArray ->
                if (other.any !is FloatArray || !this.any.contentEquals(other.any))
                    return false
            is DoubleArray ->
                if (other.any !is DoubleArray || !this.any.contentEquals(other.any))
                    return false
            else ->
                if (this.any != other.any)
                    return false
        }

        when (this.nullableAny) {
            is Array<*> ->
                if (other.nullableAny !is Array<*> || !this.nullableAny.contentDeepEquals(other.nullableAny))
                    return false
            is BooleanArray ->
                if (other.nullableAny !is BooleanArray || !this.nullableAny.contentEquals(other.nullableAny))
                    return false
            is ByteArray ->
                if (other.nullableAny !is ByteArray || !this.nullableAny.contentEquals(other.nullableAny))
                    return false
            is CharArray ->
                if (other.nullableAny !is CharArray || !this.nullableAny.contentEquals(other.nullableAny))
                    return false
            is ShortArray ->
                if (other.nullableAny !is ShortArray || !this.nullableAny.contentEquals(other.nullableAny))
                    return false
            is IntArray ->
                if (other.nullableAny !is IntArray || !this.nullableAny.contentEquals(other.nullableAny))
                    return false
            is LongArray ->
                if (other.nullableAny !is LongArray || !this.nullableAny.contentEquals(other.nullableAny))
                    return false
            is FloatArray ->
                if (other.nullableAny !is FloatArray || !this.nullableAny.contentEquals(other.nullableAny))
                    return false
            is DoubleArray ->
                if (other.nullableAny !is DoubleArray || !this.nullableAny.contentEquals(other.nullableAny))
                    return false
            else ->
                if (this.nullableAny != other.nullableAny)
                    return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = when (any) {
            is Array<*> -> any.contentDeepHashCode()
            is BooleanArray -> any.contentHashCode()
            is ByteArray -> any.contentHashCode()
            is CharArray -> any.contentHashCode()
            is ShortArray -> any.contentHashCode()
            is IntArray -> any.contentHashCode()
            is LongArray -> any.contentHashCode()
            is FloatArray -> any.contentHashCode()
            is DoubleArray -> any.contentHashCode()
            else -> any.hashCode()
        }

        result = result * 31 + when (nullableAny) {
            is Array<*> -> nullableAny.contentDeepHashCode()
            is BooleanArray -> nullableAny.contentHashCode()
            is ByteArray -> nullableAny.contentHashCode()
            is CharArray -> nullableAny.contentHashCode()
            is ShortArray -> nullableAny.contentHashCode()
            is IntArray -> nullableAny.contentHashCode()
            is LongArray -> nullableAny.contentHashCode()
            is FloatArray -> nullableAny.contentHashCode()
            is DoubleArray -> nullableAny.contentHashCode()
            else -> nullableAny.hashCode()
        }

        return result
    }

    override fun toString(): String {
        return StringBuilder()
            .append("AnyArrayHolder(")
            .append("any=")
            .append(
                when (any) {
                    is Array<*> -> any.contentDeepToString()
                    is BooleanArray -> any.contentToString()
                    is ByteArray -> any.contentToString()
                    is CharArray -> any.contentToString()
                    is ShortArray -> any.contentToString()
                    is IntArray -> any.contentToString()
                    is LongArray -> any.contentToString()
                    is FloatArray -> any.contentToString()
                    is DoubleArray -> any.contentToString()
                    else -> any.toString()
                }
            )
            .append(", ")
            .append("nullableAny=")
            .append(
                when (nullableAny) {
                    is Array<*> -> nullableAny.contentDeepToString()
                    is BooleanArray -> nullableAny.contentToString()
                    is ByteArray -> nullableAny.contentToString()
                    is CharArray -> nullableAny.contentToString()
                    is ShortArray -> nullableAny.contentToString()
                    is IntArray -> nullableAny.contentToString()
                    is LongArray -> nullableAny.contentToString()
                    is FloatArray -> nullableAny.contentToString()
                    is DoubleArray -> nullableAny.contentToString()
                    else -> nullableAny.toString()
                }
            )
            .append(")")
            .toString()
    }
}
