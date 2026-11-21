// POKO_ANNOTATION_NAME: api/CustomPoko
package api

annotation class CustomPoko {
    annotation class Redact(val mask: Int = 0)
}

@Suppress("Unused")
@CustomPoko class CustomRedactNonStringMask(
    @CustomPoko.Redact(mask = 5) val secret: String,
)

fun box(): String {
    val instance = CustomRedactNonStringMask("secret")
    return if ("$instance" == "CustomRedactNonStringMask(secret=***)") "OK" else "$instance"
}
