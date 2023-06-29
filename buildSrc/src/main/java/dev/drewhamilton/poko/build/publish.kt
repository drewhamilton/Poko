package dev.drewhamilton.poko.build

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

fun Project.setUpLocalSigning() {
    val isCi = System.getenv()["CI"] == "true"
    if (!isCi) {
        extra["signing.keyId"] = findProperty("personalGpgKeyId") ?: "x"
        extra["signing.password"] = findProperty("personalGpgPassword") ?: "x"
        extra["signing.secretKeyRingFile"] = findProperty("personalGpgKeyringFile") ?: "x"
    }
}
