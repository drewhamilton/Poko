package dev.drewhamilton.poko.build

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

interface PokoBuildExtension {
    fun publishing(pomName: String)
    fun generateBuildConfig(basePackage: String)

    /** Ensure compatibility with old Gradle and Kotlin versions. */
    fun enableBackwardsCompatibility(
        // Defaults should generally be the lowest language version still supported by the latest
        // Kotlin compiler. This would typically only require one parameter, but Kotlin 1.9 is
        // still supported for JVM targets.
        lowestSupportedKotlinVersion: KotlinVersion = KotlinVersion.KOTLIN_2_0,
        lowestSupportedKotlinJvmVersion: KotlinVersion = KotlinVersion.KOTLIN_1_9,
    )
}
