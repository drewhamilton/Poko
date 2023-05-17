package api

import dev.drewhamilton.poko.ExperimentalArrayContentSupport
import dev.drewhamilton.poko.Poko
import dev.drewhamilton.poko.ReadArrayContent

@Suppress("Unused")
@OptIn(ExperimentalArrayContentSupport::class)
@Poko class ArrayHolder(
    @ReadArrayContent val arrayReferenceType: Array<String>,
    @ReadArrayContent val nullableArrayReferenceType: Array<String>?,
    @ReadArrayContent val arrayPrimitiveType: IntArray,
    @ReadArrayContent val nullableArrayPrimitiveType: IntArray?,
    // TODO: more
)
