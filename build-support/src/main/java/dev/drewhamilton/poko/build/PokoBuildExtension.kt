package dev.drewhamilton.poko.build

interface PokoBuildExtension {
    fun publishing(pomDescription: String)
    fun generateArtifactInfo(basePackage: String)
}
