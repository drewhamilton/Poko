plugins {
    alias(libs.plugins.kotlin.jvm)
    id("dev.drewhamilton.poko")
    `java-test-fixtures`
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}
