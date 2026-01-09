import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

pokoBuild {
    publishing("Poko Annotations")
    enableBackwardsCompatibility()
}

kotlin {
    jvm()

    js().nodejs()

    mingwX64()

    linuxArm64()
    linuxX64()

    iosArm64()
    iosSimulatorArm64()
    @Suppress("DEPRECATION")
    iosX64()

    macosArm64()
    @Suppress("DEPRECATION")
    macosX64()

    tvosArm64()
    tvosSimulatorArm64()
    @Suppress("DEPRECATION")
    tvosX64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs().nodejs()
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi().nodejs()

    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    @Suppress("DEPRECATION")
    watchosX64()

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
}
