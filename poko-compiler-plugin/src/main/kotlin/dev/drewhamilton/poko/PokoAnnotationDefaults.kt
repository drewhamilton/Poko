package dev.drewhamilton.poko

// Each value must match its annotation's declared default; IR can't read another module's defaults.
internal object PokoAnnotationDefaults {
    const val REDACT_MASK = "***"
}
