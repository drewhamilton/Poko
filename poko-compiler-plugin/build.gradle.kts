import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.ksp)
}

pokoBuild {
    publishing("Poko Compiler Plugin")
    generateBuildConfig("dev.drewhamilton.poko")
}

dependencies {
    // The stdlib and compiler APIs will be provided by the enclosing Kotlin compiler environment.
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlin.embeddableCompiler)

    compileOnly(libs.autoService.annotations)
    ksp(libs.autoService.ksp)

    testImplementation(project(":poko-annotations"))
    testImplementation(libs.kotlin.embeddableCompiler)
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
    testImplementation(libs.testParameterInjector)
    testImplementation(libs.kotlinCompileTestingFork)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
