package dev.drewhamilton.poko.build

interface PokoBuildExtension {
    fun publishing(pomName: String)
    fun generateBuildConfig(basePackage: String)

    /** Ensure compatibility with old Gradle versions. */
    fun enableBackwardsCompatibility()
}
