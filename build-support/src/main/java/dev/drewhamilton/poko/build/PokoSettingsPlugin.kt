package dev.drewhamilton.poko.build

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

@Suppress("unused") // Invoked reflectively by Gradle.
class PokoSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
        target.gradle.allprojects {
            pluginManager.apply("dev.drewhamilton.poko.build")
        }
    }
}
