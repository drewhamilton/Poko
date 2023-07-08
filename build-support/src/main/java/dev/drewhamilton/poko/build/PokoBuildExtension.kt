package dev.drewhamilton.poko.build

interface PokoBuildExtension {
    fun publishing()
    fun generateArtifactInfo(basePackage: String)
}
