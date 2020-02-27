package dev.drewhamilton.safe.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class SafeGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // TODO: Decide on plugin block name
        target.extensions.create("safeClasses", SafePluginExtension::class.java)
    }
}
