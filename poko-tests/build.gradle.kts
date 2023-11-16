import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

fun KotlinMultiplatformExtension.jvm(
  version: Int,
  baseName: String = "jvm",
): KotlinJvmTarget {
  return jvm(baseName.replace("jvm", "jvm$version")) {
    compilations.configureEach {
      jvmToolchain(version)
    }

    // Dummy value required to disambiguate these targets' configurations.
    // See https://kotlinlang.org/docs/multiplatform-set-up-targets.html#distinguish-several-targets-for-one-platform
    attributes.attribute(Attribute.of("com.example.JvmTarget", Int::class.javaObjectType), version)
  }
}

kotlin {
  jvm(8)
  jvm(11)
  jvm(17)
  // Build JDK which should be latest:
  jvm()

  js {
    nodejs()
    // Produce a JS file for performance tests.
    binaries.executable()
  }

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

  wasmJs().nodejs()

  watchosArm32()
  watchosArm64()
  watchosDeviceArm64()
  watchosSimulatorArm64()
  watchosX64()

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
