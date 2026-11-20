plugins {
    pluginDevKit("compiler-plugin")
}

pokoBuild {
    publishing("Poko Compiler Plugin")
    generateBuildConfig("dev.drewhamilton.poko")
}

pluginDevKit {
    componentRegistrar = "dev.drewhamilton.poko.PokoCompilerPluginRegistrar"
    commandLineProcessor = "dev.drewhamilton.poko.PokoCommandLineProcessor"
    generateTestsEnabled = false
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
