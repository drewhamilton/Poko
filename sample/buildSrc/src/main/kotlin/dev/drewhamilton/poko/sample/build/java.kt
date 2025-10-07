package dev.drewhamilton.poko.sample.build

import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val resolvedJavaVersion: JavaVersion = JavaVersion.VERSION_11

val kotlinJvmTarget: JvmTarget = when (resolvedJavaVersion) {
    JavaVersion.VERSION_1_8 -> JvmTarget.JVM_1_8
    else -> JvmTarget.valueOf("JVM_${resolvedJavaVersion.majorVersion}")
}
