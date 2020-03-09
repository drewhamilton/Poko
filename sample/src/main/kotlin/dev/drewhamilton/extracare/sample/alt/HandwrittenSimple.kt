package dev.drewhamilton.extracare.sample.alt

import java.util.Objects

/**
 * Source code equivalent to the desired end state of [Sample], useful for comparing bytecode.
 */
class HandwrittenSample private constructor(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
) {

    override fun toString() =
        "HandwrittenSample(int=$int, requiredString=$requiredString, optionalString=$optionalString)"

    override fun equals(other: Any?) = other is HandwrittenSample &&
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

        fun build(): HandwrittenSample {
            val requiredString = requiredString
            checkNotNull(requiredString) { "Required value requiredString was null" }

            return HandwrittenSample(
                int,
                requiredString,
                optionalString
            )
        }
    }
}

@Suppress("FunctionName", "unused")
@JvmSynthetic
fun HandwrittenSample(initializer: HandwrittenSample.Builder.() -> Unit) =
    HandwrittenSample.Builder().apply(initializer).build()
