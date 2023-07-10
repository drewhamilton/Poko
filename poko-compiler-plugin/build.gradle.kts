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

// https://jakewharton.com/build-on-latest-java-test-through-lowest-java/
for (javaVersion in 8..18) {
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

    // JDK 10 is flaky on CI:
    val isCi = System.getenv()["CI"] == "true"
    if (isCi && javaVersion == 10)
        continue

    tasks.named("check").configure { dependsOn(jdkTest) }
}
