import com.google.devtools.ksp.gradle.KspTask
import dev.drewhamilton.poko.build.generateArtifactInfo
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dokka)
}

extra.apply {
    set("artifactName", project.property("publishGradlePluginArtifact")!!)
    set("pomName", "Poko Gradle Plugin")
    set("gradlePluginDomainObjectName", "poko")
}
apply(from = "../publish.gradle")

generateArtifactInfo(
    basePackage = "dev.drewhamilton.poko.gradle",
    DokkaTask::class, Jar::class, KspTask::class,
)

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-progressive")
    }
}

gradlePlugin {
    plugins {
        val gradlePluginDomainObjectName: String by extra
        create(gradlePluginDomainObjectName) {
            id = "dev.drewhamilton.poko"
            implementationClass = "dev.drewhamilton.poko.gradle.PokoGradlePlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlin.gradleApi)
    compileOnly(libs.kotlin.gradle)

    implementation(libs.autoService.annotations)
    ksp(libs.autoService.ksp)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
