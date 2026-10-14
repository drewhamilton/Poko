package api

annotation class NoMaskPoko {
    annotation class Redact
}

@Suppress("Unused")
@NoMaskPoko class CustomRedactNoMask(
    val user: String,
    @NoMaskPoko.Redact val password: String,
)
