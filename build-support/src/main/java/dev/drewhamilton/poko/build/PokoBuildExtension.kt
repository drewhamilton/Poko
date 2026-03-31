package dev.drewhamilton.poko.build

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

interface PokoBuildExtension {
    fun publishing(pomName: String)
    fun generateBuildConfig(basePackage: String)

    /** Ensure compatibility with old Gradle and Kotlin versions. */
    fun enableBackwardsCompatibility(
        lowestSupportedKotlinVersion: KotlinVersion = KotlinVersion.KOTLIN_2_0,
    )
}
