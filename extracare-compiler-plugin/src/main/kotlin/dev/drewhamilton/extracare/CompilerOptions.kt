package dev.drewhamilton.extracare

import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal object CompilerOptions {
    val ENABLED = CompilerConfigurationKey<Boolean>("enabled")
    val DATA_API_ANNOTATION = CompilerConfigurationKey<String>("dataApiAnnotation")
}
