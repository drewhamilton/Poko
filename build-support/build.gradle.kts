plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradleApi)
    implementation(libs.plugin.mavenPublish)
    implementation(libs.plugin.dokka)
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
