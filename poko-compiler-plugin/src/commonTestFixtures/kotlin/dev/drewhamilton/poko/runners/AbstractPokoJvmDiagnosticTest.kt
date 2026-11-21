package dev.drewhamilton.poko.runners

import org.jetbrains.kotlin.compiler.plugin.devkit.runners.DevKitJvmDiagnosticTest
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.RUN_PIPELINE_TILL
import org.jetbrains.kotlin.test.services.TestPhase

open class AbstractPokoJvmDiagnosticTest : DevKitJvmDiagnosticTest({
    configureTests()
    defaultDirectives {
        RUN_PIPELINE_TILL with TestPhase.FRONTEND
    }
})
