package poko

import dev.drewhamilton.poko.ArrayContentBased
import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class ArrayHolder(
    @ArrayContentBased val stringArray: Array<String>,
    @ArrayContentBased val nullableStringArray: Array<String>?,
    @ArrayContentBased val booleanArray: BooleanArray,
    @ArrayContentBased val nullableBooleanArray: BooleanArray?,
    @ArrayContentBased val byteArray: ByteArray,
    @ArrayContentBased val nullableByteArray: ByteArray?,
    @ArrayContentBased val charArray: CharArray,
    @ArrayContentBased val nullableCharArray: CharArray?,
    @ArrayContentBased val shortArray: ShortArray,
    @ArrayContentBased val nullableShortArray: ShortArray?,
    @ArrayContentBased val intArray: IntArray,
    @ArrayContentBased val nullableIntArray: IntArray?,
    @ArrayContentBased val longArray: LongArray,
    @ArrayContentBased val nullableLongArray: LongArray?,
    @ArrayContentBased val floatArray: FloatArray,
    @ArrayContentBased val nullableFloatArray: FloatArray?,
    @ArrayContentBased val doubleArray: DoubleArray,
    @ArrayContentBased val nullableDoubleArray: DoubleArray?,
    @ArrayContentBased val nestedStringArray: Array<Array<String>>,
    @ArrayContentBased val nestedIntArray: Array<IntArray>,
)
