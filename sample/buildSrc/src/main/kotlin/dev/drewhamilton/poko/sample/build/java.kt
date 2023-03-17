package dev.drewhamilton.poko.sample.build

import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ciJavaVersion: Int? = System.getenv()["ci_java_version"]?.toInt()

val jvmToolchainLanguageVersion: JavaLanguageVersion? = ciJavaVersion?.let {
    JavaLanguageVersion.of(ciJavaVersion.toInt())
}

val resolvedJavaVersion: JavaVersion = when (ciJavaVersion) {
    null -> JavaVersion.VERSION_11
    8, 9, 10 -> JavaVersion.valueOf("VERSION_1_$ciJavaVersion")
    else -> JavaVersion.valueOf("VERSION_$ciJavaVersion")
}

val kotlinJvmTarget: JvmTarget = when (resolvedJavaVersion) {
    JavaVersion.VERSION_1_8 -> JvmTarget.JVM_1_8
    else -> JvmTarget.valueOf("JVM_${resolvedJavaVersion.majorVersion}")
}
