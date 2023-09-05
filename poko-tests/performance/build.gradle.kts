plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
    testImplementation(libs.asm.util)
}

tasks.named("test") {
    dependsOn(":poko-tests:compileKotlinJvm")
}
