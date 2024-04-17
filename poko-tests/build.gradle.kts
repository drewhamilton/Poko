import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

val jvmToolchainVersion: Int? = System.getenv()["poko_tests_jvm_toolchain_version"]?.toInt()

fun <T : KotlinTarget> T.applyK2(): T {
  compilations.configureEach {
    compilerOptions.options.languageVersion.set(KotlinVersion.KOTLIN_2_0)
  }

  // Dummy value required to disambiguate these targets' configurations.
  // See https://kotlinlang.org/docs/multiplatform-set-up-targets.html#distinguish-several-targets-for-one-platform
  attributes.attribute(Attribute.of("com.example.useK2", Boolean::class.javaObjectType), true)

  return this
}

kotlin {
  jvmToolchainVersion?.let { jvmToolchain(it) }

  jvm()
  jvm("jvmK2").applyK2()

  js {
    nodejs()
    // Produce a JS file for performance tests.
    binaries.executable()
  }
  js("jsK2").applyK2().nodejs()

  mingwX64()
  mingwX64("mingwX64K2").applyK2()

  linuxArm64()
  linuxArm64("linuxArm64K2").applyK2()
  linuxX64()
  linuxX64("linuxX64K2").applyK2()

  iosArm64()
  iosArm64("iosArm64K2").applyK2()
  iosSimulatorArm64()
  iosSimulatorArm64("iosSimulatorArm64K2").applyK2()
  iosX64()
  iosX64("iosX64K2").applyK2()

  macosArm64()
  macosArm64("macosArm64K2").applyK2()
  macosX64()
  macosX64("macosX64K2").applyK2()

  tvosArm64()
  tvosArm64("tvosArm64K2").applyK2()
  tvosX64()
  tvosX64("tvosX64K2").applyK2()
  tvosSimulatorArm64()
  tvosSimulatorArm64("tvosSimulatorArm64K2").applyK2()

  wasmJs().nodejs()
  wasmJs("wasmJsK2").applyK2().nodejs()

  watchosArm32()
  watchosArm32("watchosArm32K2").applyK2()
  watchosArm64()
  watchosArm64("watchosArm64K2").applyK2()
  watchosDeviceArm64()
  watchosDeviceArm64("watchosDeviceArm64K2").applyK2()
  watchosSimulatorArm64()
  watchosSimulatorArm64("watchosSimulatorArm64K2").applyK2()
  watchosX64()
  watchosX64("watchosX64K2").applyK2()

  androidNativeArm32()
  androidNativeArm32("androidNativeArm32K2").applyK2()
  androidNativeArm64()
  androidNativeArm64("androidNativeArm64K2").applyK2()

  androidNativeX86()
  androidNativeX86("androidNativeX86K2").applyK2()
  androidNativeX64()
  androidNativeX64("androidNativeX64K2").applyK2()

  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":poko-annotations"))
      }
    }
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
      }
    }
  }
}

dependencies {
  add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, project(":poko-compiler-plugin"))
  add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, project(":poko-compiler-plugin"))
}
