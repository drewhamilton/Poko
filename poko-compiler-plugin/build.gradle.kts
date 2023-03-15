import com.google.devtools.ksp.gradle.KspTask
import groovy.lang.Closure
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
}

extra.apply {
    set("artifactName", project.property("publish_compiler_plugin_artifact")!!)
    set("pomName", "Poko Compiler Plugin")
    set("basePackage", "dev.drewhamilton.poko")
}
apply(from = "../publish.gradle")
apply(from = "../info.gradle")

dependencies {
    compileOnly(libs.kotlin.embeddableCompiler)

    implementation(libs.autoService.annotations)
    ksp(libs.autoService.ksp)

    testImplementation(project(":poko-annotations"))
    testImplementation(libs.kotlin.embeddableCompiler)
    testImplementation(libs.kotlin.compileTestingFork)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

//compileTestKotlin {
//    kotlinOptions {
//        freeCompilerArgs = ["-Xinline-classes"]
//    }
//}

// https://jakewharton.com/build-on-latest-java-test-through-lowest-java/
for (javaVersion in 8..17) {
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

val dependsOnCopyInfoTemplate: (Closure<*>) by extra
dependsOnCopyInfoTemplate(DokkaTask::class.java)
dependsOnCopyInfoTemplate(Jar::class.java)
dependsOnCopyInfoTemplate(KspTask::class.java)
