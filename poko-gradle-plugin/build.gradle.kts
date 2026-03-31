import com.github.gmazzo.buildconfig.BuildConfigExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

// Keep these in sync with each other. See https://docs.gradle.org/current/userguide/compatibility.html#kotlin.
private val minimumGradleVersion = "9.0.0"
private val minimumGradleKotlinVersion = KotlinVersion.KOTLIN_2_2
private val minimumGradleMinimumJavaVersion = 17
private val minimumGradleMaximumJavaVersion = 24

pokoBuild {
    publishing("Poko Gradle Plugin")
    generateBuildConfig("dev.drewhamilton.poko.gradle")
    enableBackwardsCompatibility(lowestSupportedKotlinVersion = minimumGradleKotlinVersion)
}

gradlePlugin {
    plugins {
        create("poko") {
            id = "dev.drewhamilton.poko"
            implementationClass = "dev.drewhamilton.poko.gradle.PokoGradlePlugin"
        }
    }
}

kotlin {
    jvmToolchain(minimumGradleMaximumJavaVersion)
}

project.tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = minimumGradleMinimumJavaVersion.toString()
    targetCompatibility = minimumGradleMinimumJavaVersion.toString()
}
project.tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(minimumGradleMinimumJavaVersion.toString()))
        freeCompilerArgs.add("-Xjdk-release=$minimumGradleMinimumJavaVersion")
    }
}

configurations.configureEach {
    if (isCanBeConsumed) {
        attributes {
            attribute(
                GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                objects.named(minimumGradleVersion),
            )
        }
    }
}

// Workaround for clash between `signature` and `archives`; remove when bumping to Gradle 10:
configurations.archives {
    attributes {
        attribute(Attribute.of("deprecated", String::class.java), "true")
    }
}

with(the<BuildConfigExtension>()) {
    sourceSets.named("test") {
        buildConfigField(String::class.java, "MINIMUM_GRADLE_VERSION", minimumGradleVersion)
    }
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}

dependencies {
    compileOnly(libs.kotlin.gradleApi)
    compileOnly(libs.android.gradleApi)

    testImplementation(libs.junit)
    testImplementation(libs.assertk)
    testImplementation(libs.testParameterInjector)
    testImplementation(gradleTestKit())
}

tasks.test {
    inputs.dir(file("src/test/fixtures"))
    dependsOn(
        ":poko-annotations:publishAllPublicationsToTestingRepository",
        ":poko-compiler-plugin:publishAllPublicationsToTestingRepository",
        ":poko-gradle-plugin:publishAllPublicationsToTestingRepository",
    )
    jvmArgs(
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
    )
}
