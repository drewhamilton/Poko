package api

import dev.drewhamilton.poko.ExperimentalArrayContentSupport
import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.ReadArrayContent

@Suppress("Unused")
@OptIn(ExperimentalArrayContentSupport::class)
@Poko class ArrayHolder(
    @ReadArrayContent val stringArray: Array<String>,
    @ReadArrayContent val nullableStringArray: Array<String>?,
    @ReadArrayContent val booleanArray: BooleanArray,
    @ReadArrayContent val nullableBooleanArray: BooleanArray?,
    @ReadArrayContent val byteArray: ByteArray,
    @ReadArrayContent val nullableByteArray: ByteArray?,
    @ReadArrayContent val charArray: CharArray,
    @ReadArrayContent val nullableCharArray: CharArray?,
    @ReadArrayContent val shortArray: ShortArray,
    @ReadArrayContent val nullableShortArray: ShortArray?,
    @ReadArrayContent val intArray: IntArray,
    @ReadArrayContent val nullableIntArray: IntArray?,
    @ReadArrayContent val longArray: LongArray,
    @ReadArrayContent val nullableLongArray: LongArray?,
    @ReadArrayContent val floatArray: FloatArray,
    @ReadArrayContent val nullableFloatArray: FloatArray?,
    @ReadArrayContent val doubleArray: DoubleArray,
    @ReadArrayContent val nullableDoubleArray: DoubleArray?,
    // TODO: more
)
