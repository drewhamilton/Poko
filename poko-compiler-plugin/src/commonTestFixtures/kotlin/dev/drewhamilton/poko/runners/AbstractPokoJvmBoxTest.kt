package dev.drewhamilton.poko.runners

import org.jetbrains.kotlin.compiler.plugin.devkit.runners.DevKitJvmBoxTest

open class AbstractPokoJvmBoxTest: DevKitJvmBoxTest({ configureTests() })
