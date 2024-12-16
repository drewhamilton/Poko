import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

pokoBuild {
  publishing("Poko Annotations")
}

kotlin {
  jvm()

  js().nodejs()

  mingwX64()

  linuxArm64()
  linuxX64()

  iosArm64()
  iosSimulatorArm64()
  iosX64()

  macosArm64()
  macosX64()

  tvosArm64()
  tvosX64()
  tvosSimulatorArm64()

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs().nodejs()
  @OptIn(ExperimentalWasmDsl::class)
  wasmWasi().nodejs()

  watchosArm32()
  watchosArm64()
  watchosDeviceArm64()
  watchosSimulatorArm64()
  watchosX64()

  androidNativeArm32()
  androidNativeArm64()
  androidNativeX86()
  androidNativeX64()
}
