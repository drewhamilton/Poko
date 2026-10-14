package api

annotation class CustomPoko {
    annotation class Redact(val mask: Int = 0)
}

@Suppress("Unused")
@CustomPoko class CustomRedactNonStringMask(
    @CustomPoko.Redact(mask = 5) val secret: String,
)
