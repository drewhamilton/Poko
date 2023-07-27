plugins {
    alias(libs.plugins.kotlin.jvm)
    id("dev.drewhamilton.poko")
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}
