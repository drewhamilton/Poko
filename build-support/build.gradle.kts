import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradleApi)
    implementation(libs.plugin.buildconfig)
    implementation(libs.plugin.mavenPublish)
    implementation(libs.plugin.dokka)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

gradlePlugin {
    plugins {
        create("build") {
            id = "dev.drewhamilton.poko.build"
            implementationClass = "dev.drewhamilton.poko.build.PokoBuildPlugin"
        }
        create("settings") {
            id = "dev.drewhamilton.poko.settings"
            implementationClass = "dev.drewhamilton.poko.build.PokoSettingsPlugin"
        }
    }
}
