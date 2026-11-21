// POKO_ANNOTATION_NAME: api/NoMaskPoko
package api

annotation class NoMaskPoko {
    annotation class Redact
}

@Suppress("Unused")
@NoMaskPoko class CustomRedactNoMask(
    val user: String,
    @NoMaskPoko.Redact val password: String,
)

fun box(): String {
    val instance = CustomRedactNoMask("bob", "hunter2")
    return if ("$instance" == "CustomRedactNoMask(user=bob, password=***)") "OK" else "$instance"
}
