package dev.drewhamilton.poko

import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.CompilerConfiguration

actual fun createCompilerConfiguration(): CompilerConfiguration = CompilerConfiguration.create()
