import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("dev.drewhamilton.poko")
}

poko {
    pokoAnnotation.set("dev/drewhamilton/poko/sample/jvm/Poko")
    enabled.set(true)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        val kotlinJvmTarget: JvmTarget by rootProject.extra
        jvmTarget.set(kotlinJvmTarget)
        freeCompilerArgs.add("-progressive")
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
