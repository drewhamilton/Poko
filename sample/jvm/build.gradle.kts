plugins {
    alias(libs.plugins.kotlin.jvm)
    id("dev.drewhamilton.poko")
}

poko {
    enabled.set(true)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}
