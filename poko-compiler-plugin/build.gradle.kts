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
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
    testImplementation(libs.testParameterInjector)
    val kctVersion = libs.versions.kotlinCompileTesting.get()
    if (kctVersion.endsWith("-local")) {
        // Include the local KCT jar and its copied dependencies
        val kctName = libs.kotlin.compileTesting.get().name
        testImplementation(files("libs/$kctName-$kctVersion.jar"))
        // Copied from KCT's build.gradle:
        testImplementation("com.squareup.okio:okio:3.3.0")
        testImplementation("io.github.classgraph:classgraph:4.8.158")
        testImplementation("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:${libs.versions.kotlin.get()}")
    } else {
        testImplementation(libs.kotlin.compileTesting)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
