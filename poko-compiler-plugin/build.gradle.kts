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
    testImplementation(libs.asm.util)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

// https://jakewharton.com/build-on-latest-java-test-through-lowest-java/
// The normal test task will run on the latest JDK.
for (javaVersion in listOf(8, 11, 17)) {
    val jdkTest = tasks.register<Test>("testJdk$javaVersion") {
        val javaToolchains  = project.extensions.getByType<JavaToolchainService>()
        javaLauncher.set(
            javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(javaVersion))
            }
        )

        description = "Runs the test suite on JDK $javaVersion"
        group = LifecycleBasePlugin.VERIFICATION_GROUP

        val testTask = tasks.getByName("test") as Test
        classpath = testTask.classpath
        testClassesDirs = testTask.testClassesDirs
    }

    tasks.named("check").configure { dependsOn(jdkTest) }
}
