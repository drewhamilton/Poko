plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

pokoBuild {
  publishing()
}

fun <T : org.jetbrains.kotlin.gradle.plugin.KotlinTarget> T.applyK2(): T {
  compilations.configureEach {
    compilerOptions.options.languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
  }

  // Dummy value required to disambiguate these targets' configurations.
  // See https://kotlinlang.org/docs/multiplatform-set-up-targets.html#distinguish-several-targets-for-one-platform
  attributes.attribute(Attribute.of("com.example.useK2", Boolean::class.javaObjectType), true)

  return this
}

kotlin {
  jvm()

  js().nodejs()
  js("jsK2").applyK2().nodejs()

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
}
