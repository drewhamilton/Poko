package dev.drewhamilton.poko

import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal object CompilerOptions {
    val ENABLED = CompilerConfigurationKey<Boolean>(BuildConfig.POKO_ENABLED_OPTION_NAME)
    val POKO_ANNOTATION = CompilerConfigurationKey<String>(BuildConfig.POKO_ANNOTATION_OPTION_NAME)
    val POKO_PLUGIN_ARGS =
        CompilerConfigurationKey<Map<String, String>>(BuildConfig.POKO_PLUGIN_ARGS_OPTION_NAME)
}
