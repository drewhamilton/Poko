import dev.drewhamilton.poko.build.generateArtifactInfo
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

generateArtifactInfo(
    basePackage = "dev.drewhamilton.poko.gradle",
)

kotlin {
    explicitApi = ExplicitApiMode.Strict
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-progressive")
    }
}

gradlePlugin {
    plugins {
        create("poko") {
            id = "dev.drewhamilton.poko"
            implementationClass = "dev.drewhamilton.poko.gradle.PokoGradlePlugin"
        }
    }
}

dependencies {
    compileOnly(libs.kotlin.gradleApi)

    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}
