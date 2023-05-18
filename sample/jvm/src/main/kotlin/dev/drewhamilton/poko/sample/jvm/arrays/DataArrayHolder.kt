package dev.drewhamilton.poko.sample.jvm.arrays

@Suppress("unused", "ArrayInDataClass")
data class DataArrayHolder(
    val id: String,
    val array: Array<String>,
)
