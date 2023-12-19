package dev.drewhamilton.poko.build

interface PokoBuildExtension {
    fun publishing(pomName: String)
    fun generateBuildConfig(basePackage: String)
}
