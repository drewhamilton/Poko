package dev.drewhamilton.poko

import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal object CompilerOptions {
    val ENABLED = CompilerConfigurationKey<Boolean>(BuildConfig.POKO_ENABLED_OPTION_NAME)
    val POKO_ANNOTATION = CompilerConfigurationKey<String>(BuildConfig.POKO_ANNOTATION_OPTION_NAME)
    val FIR_IDE_MODE = CompilerConfigurationKey<FirIdeMode>(BuildConfig.POKO_FIR_IDE_MODE_OPTION_NAME)
}

internal enum class FirIdeMode {
    ALL,
    CHECKERS_ONLY,
    NONE,
}
