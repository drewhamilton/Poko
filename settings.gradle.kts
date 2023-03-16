pluginManagement {
    repositories {
        mavenCentral()

        // KSP:
        google()
    }
}

rootProject.name = "Poko"

include(
    ":poko-compiler-plugin",
    ":poko-annotations",
    ":poko-gradle-plugin",
)
