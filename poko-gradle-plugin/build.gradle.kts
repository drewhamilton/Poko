import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    compilerOptions {
        /* Versions used here should be no more than one Kotlin release ahead of the Kotlin version embedded with the
        oldest supported Gradle version. Gradle 8.11 embeds Kotlin 2.0 so this plugin will be compatible if using Kotlin
        2.1 or older: https://docs.gradle.org/current/userguide/compatibility.html */
        apiVersion = KotlinVersion.KOTLIN_2_1
        languageVersion = KotlinVersion.KOTLIN_2_1
    }
}

tasks.withType(KotlinCompile::class).configureEach {
    compilerOptions {
        progressiveMode = false
    }
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
