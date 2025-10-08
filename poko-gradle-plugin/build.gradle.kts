plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

pokoBuild {
    publishing("Poko Gradle Plugin")
    generateBuildConfig("dev.drewhamilton.poko.gradle")
    enableBackwardsCompatibility()
}

gradlePlugin {
    plugins {
        create("poko") {
            id = "dev.drewhamilton.poko"
            implementationClass = "dev.drewhamilton.poko.gradle.PokoGradlePlugin"
        }
    }
}

// HEY! If you update the minimum-supported Gradle version check to see if the Kotlin language version
// can be bumped in PokoBuildPlugin.kt. See https://docs.gradle.org/current/userguide/compatibility.html#kotlin.
configurations.apiElements {
    attributes {
        attribute(
            GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
            objects.named(GradlePluginApiVersion::class, "8.11"),
        )
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
