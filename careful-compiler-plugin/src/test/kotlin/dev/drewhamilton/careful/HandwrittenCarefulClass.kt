package dev.drewhamilton.careful

import java.util.Objects

class HandwrittenCarefulClass private constructor(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
) {

    override fun toString() =
        "HandwrittenCarefulClass(int=$int, requiredString=$requiredString, optionalString=$optionalString)"

    override fun equals(other: Any?) = other is HandwrittenCarefulClass &&
            int == other.int &&
            requiredString == other.requiredString &&
            optionalString == other.optionalString

    override fun hashCode() = Objects.hash(int, requiredString, optionalString)

    class Builder {
        @set:JvmSynthetic var int: Int = 0
        @set:JvmSynthetic var requiredString: String? = null
        @set:JvmSynthetic var optionalString: String? = null

        fun setInt(int: Int) = apply { this.int = int }
        fun setRequiredString(requiredString: String?) = apply { this.requiredString = requiredString }
        fun setOptionalString(optionalString: String?) = apply { this.optionalString = optionalString }

        fun build(): HandwrittenCarefulClass {
            val requiredString = requiredString
            checkNotNull(requiredString) { "Required value requiredString was null" }

            return HandwrittenCarefulClass(int, requiredString, optionalString)
        }
    }
}

@Suppress("TestFunctionName")
@JvmSynthetic
fun HandwrittenCarefulClass(initializer: HandwrittenCarefulClass.Builder.() -> Unit) =
    HandwrittenCarefulClass.Builder().apply(initializer).build()
