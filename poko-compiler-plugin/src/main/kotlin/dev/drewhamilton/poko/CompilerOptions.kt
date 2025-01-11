package dev.drewhamilton.poko

import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal object CompilerOptions {
    val ENABLED = CompilerConfigurationKey<Boolean>("enabled")
    val POKO_ANNOTATION = CompilerConfigurationKey<String>("pokoAnnotation")
    val POKO_PLUGIN_ARGS = CompilerConfigurationKey<List<String>>("pokoPluginArgs")
}
