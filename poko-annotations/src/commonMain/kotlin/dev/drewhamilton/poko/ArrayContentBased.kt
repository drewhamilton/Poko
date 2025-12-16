package dev.drewhamilton.poko

/**
 * Legacy name for [Poko.ReadArrayContent].
 */
@Deprecated(
    message = "Moved to @Poko.ReadArrayContent for compatibility with custom Poko annotation",
    replaceWith = ReplaceWith("Poko.ReadArrayContent"),
    level = DeprecationLevel.ERROR,
)
public typealias ArrayContentBased = Poko.ReadArrayContent
