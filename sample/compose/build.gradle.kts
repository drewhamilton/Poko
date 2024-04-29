import dev.drewhamilton.poko.sample.build.jvmToolchainLanguageVersion
import dev.drewhamilton.poko.sample.build.resolvedJavaVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.plugin)
    id("dev.drewhamilton.poko")
}

poko {
    pokoAnnotation.set("dev/drewhamilton/poko/sample/compose/Poko")
}

if (jvmToolchainLanguageVersion != null) {
    kotlin {
        jvmToolchain {
            languageVersion.set(jvmToolchainLanguageVersion)
        }
    }
}

android {
    namespace = "dev.drewhamilton.poko.sample.compose"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    // TODO: Wrap in `if (jvmToolchainLanguageVersion == null)` from AGP 8.1
    compileOptions {
        sourceCompatibility(resolvedJavaVersion)
        targetCompatibility(resolvedJavaVersion)
    }

    kotlinOptions {
        freeCompilerArgs = listOf("-progressive")
    }

    @Suppress("UnstableApiUsage")
    buildFeatures {
        compose = true

        // Disable unused AGP features
        resValues = false
        shaders = false
        androidResources = false
    }
}

dependencies {
    implementation(libs.androidx.compose.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}

repositories {
    google()
}
