package dev.drewhamilton.poko

import dev.drewhamilton.poko.runners.AbstractPokoJvmBoxTest
import dev.drewhamilton.poko.runners.AbstractPokoJvmDiagnosticTest
import org.jetbrains.kotlin.compiler.plugin.devkit.generateDevKitTestsWithJUnit5

fun main() = generateDevKitTestsWithJUnit5 {
    testClass<AbstractPokoJvmBoxTest> { model("box") }
    testClass<AbstractPokoJvmDiagnosticTest> { model("diagnostics") }
}
