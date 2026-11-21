plugins {
    pluginDevKit("compiler-plugin")
}

pokoBuild {
    publishing("Poko Compiler Plugin")
    generateBuildConfig("dev.drewhamilton.poko")
}

pluginDevKit {
    cliVersions("2.3.20")
    ideaVersions("261")

    componentRegistrar = "dev.drewhamilton.poko.PokoCompilerPluginRegistrar"
    commandLineProcessor = "dev.drewhamilton.poko.PokoCommandLineProcessor"

    generateTestsEnabled = false
    versionHierarchy {
        splitDev(2, 4, 20, "IrValidationSeverity")
        splitDev(2, 4, 0, "CompilerConfigurationCreate")
    }
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(project(":poko-annotations"))
            implementation(libs.assertk)
            implementation(libs.testParameterInjector)
            implementation(libs.kotlinCompileTestingFork)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
