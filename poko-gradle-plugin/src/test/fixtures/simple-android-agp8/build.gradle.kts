import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library") version "8.8.2"
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin.get()
    alias(libs.plugins.poko)
}

android {
    namespace = "dev.drewhamilton.poko.gradle.test.android.agp8"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

dependencies {
    testImplementation(libs.junit)
}
