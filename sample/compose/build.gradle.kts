import dev.drewhamilton.poko.sample.build.jvmToolchainLanguageVersion
import dev.drewhamilton.poko.sample.build.resolvedJavaVersion

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("dev.drewhamilton.poko")
}

poko {
    pokoAnnotation.set("dev.drewhamilton.poko.sample.compose.Poko")
    enabled.set(true)
}

if (jvmToolchainLanguageVersion != null) {
    kotlin {
        jvmToolchain {
            languageVersion.set(jvmToolchainLanguageVersion)
        }
    }
}

android {
    compileSdkVersion(32)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(32)
    }

    if (jvmToolchainLanguageVersion == null) {
        compileOptions {
            sourceCompatibility(resolvedJavaVersion)
            targetCompatibility(resolvedJavaVersion)
        }
    }

    kotlinOptions {
        freeCompilerArgs = listOf(
            "-progressive",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true",
        )
    }

    buildFeatures {
        compose = true

        // Disable unused AGP features
        buildConfig = false
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
}

dependencies {
    implementation(libs.androidx.compose.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

repositories {
    google()
    if (android.composeOptions.kotlinCompilerExtensionVersion!!.contains("dev")) {
        logger.lifecycle("Adding Compose compiler dev repository")
        maven { url = uri("https://androidx.dev/storage/compose-compiler/repository") }
    }
}
