plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("dev.drewhamilton.poko")
}

kotlin {
    js().nodejs()

    jvm()

    // All native "desktop" platforms to ensure at least one set of native tests will run.
    linuxArm64()
    linuxX64()
    macosArm64()
    if (!libs.versions.kotlin.get().startsWith("2.5")) {
        @Suppress("DEPRECATION_ERROR")
        macosX64()
    }
    mingwX64()

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.assertk)
            }
        }
    }
}
