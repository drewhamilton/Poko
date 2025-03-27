import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

val compileMode = findProperty("pokoTests.compileMode")
when (compileMode) {
  null -> Unit // Nothing to configure

  "WITHOUT_K2" -> {
    logger.lifecycle("Building :poko-tests without K2 (language level 1.9)")
    tasks.withType<KotlinCompile>().configureEach {
      compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_1_9
        progressiveMode = false
      }
    }
  }

  "FIR_GENERATION_DISABLED" -> {
    logger.lifecycle("Building :poko-tests with FIR declaration generation disabled")
    tasks.withType<KotlinCompile>().configureEach {
      compilerOptions {
        freeCompilerArgs.addAll(
          listOf(
            "-P",
            "plugin:poko-compiler-plugin:pokoPluginArgs=poko.experimental.enableFirDeclarationGeneration=false",
          ),
        )
      }
    }
  }

  else -> throw IllegalArgumentException("Unknown pokoTests.compileMode: <$compileMode>")
}

val jvmToolchainVersion = (findProperty("pokoTests.jvmToolchainVersion") as? String)?.toInt()

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }

  jvmToolchainVersion?.let { jvmToolchain(it) }

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
