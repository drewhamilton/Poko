import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

pokoBuild {
    publishing()
    generateArtifactInfo("dev.drewhamilton.poko")
}

dependencies {
    // The stdlib and compiler APIs will be provided by the enclosing Kotlin compiler environment.
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlin.embeddableCompiler)

    compileOnly(libs.autoService.annotations)
    ksp(libs.autoService.ksp)

    testImplementation(project(":poko-annotations"))
    testImplementation(libs.kotlin.embeddableCompiler)
    testImplementation(libs.kotlin.compileTesting)
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
