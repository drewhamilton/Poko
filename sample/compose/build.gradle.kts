import dev.drewhamilton.poko.sample.build.jvmToolchainLanguageVersion
import dev.drewhamilton.poko.sample.build.resolvedJavaVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility(resolvedJavaVersion)
        targetCompatibility(resolvedJavaVersion)
    }

    kotlinOptions {
        freeCompilerArgs = listOf("-progressive")
    }

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
