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

android {
    namespace = "dev.drewhamilton.poko.sample.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility(resolvedJavaVersion)
        targetCompatibility(resolvedJavaVersion)
    }

    kotlin {
        compilerOptions {
            progressiveMode.set(true)
        }
    }

    buildFeatures {
        compose = true

        // Disable unused AGP features
        resValues = false
        shaders = false
    }

    androidResources.enable = false
}

dependencies {
    implementation(libs.androidx.compose.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}

repositories {
    google()
}
