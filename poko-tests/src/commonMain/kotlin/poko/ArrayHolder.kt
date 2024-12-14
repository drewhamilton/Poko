package poko

import dev.drewhamilton.poko.Poko

@Suppress("Unused")
@Poko class ArrayHolder(
    @Poko.ReadArrayContent val stringArray: Array<String>,
    @Poko.ReadArrayContent val nullableStringArray: Array<String>?,
    @Poko.ReadArrayContent val booleanArray: BooleanArray,
    @Poko.ReadArrayContent val nullableBooleanArray: BooleanArray?,
    @Poko.ReadArrayContent val byteArray: ByteArray,
    @Poko.ReadArrayContent val nullableByteArray: ByteArray?,
    @Poko.ReadArrayContent val charArray: CharArray,
    @Poko.ReadArrayContent val nullableCharArray: CharArray?,
    @Poko.ReadArrayContent val shortArray: ShortArray,
    @Poko.ReadArrayContent val nullableShortArray: ShortArray?,
    @Poko.ReadArrayContent val intArray: IntArray,
    @Poko.ReadArrayContent val nullableIntArray: IntArray?,
    @Poko.ReadArrayContent val longArray: LongArray,
    @Poko.ReadArrayContent val nullableLongArray: LongArray?,
    @Poko.ReadArrayContent val floatArray: FloatArray,
    @Poko.ReadArrayContent val nullableFloatArray: FloatArray?,
    @Poko.ReadArrayContent val doubleArray: DoubleArray,
    @Poko.ReadArrayContent val nullableDoubleArray: DoubleArray?,
    @Poko.ReadArrayContent val nestedStringArray: Array<Array<String>>,
    @Poko.ReadArrayContent val nestedIntArray: Array<IntArray>,
)
