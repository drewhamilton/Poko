plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

pokoBuild {
    publishing("Poko Gradle Plugin")
    generateBuildConfig("dev.drewhamilton.poko.gradle")
}

gradlePlugin {
    plugins {
        create("poko") {
            id = "dev.drewhamilton.poko"
            implementationClass = "dev.drewhamilton.poko.gradle.PokoGradlePlugin"
        }
    }
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}

dependencies {
    compileOnly(libs.kotlin.gradleApi)

    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}
