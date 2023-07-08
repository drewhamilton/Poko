import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
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

  watchosArm32()
  watchosArm64()
  watchosDeviceArm64()
  watchosSimulatorArm64()
  watchosX64()

  explicitApi = ExplicitApiMode.Strict
}
