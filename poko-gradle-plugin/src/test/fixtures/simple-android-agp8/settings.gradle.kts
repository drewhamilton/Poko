pluginManagement {
    repositories {
        maven {
            setUrl(rootDir.resolve("../../../../../build/localMaven"))
        }
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    versionCatalogs.register("libs") {
        from(files("../../../../../gradle/libs.versions.toml"))
        plugin("poko", "dev.drewhamilton.poko").version(providers.gradleProperty("pokoVersion").get())
    }

    repositories {
        maven {
            setUrl(rootDir.resolve("../../../../../build/localMaven"))
        }
        mavenCentral()
        google()
    }
}
