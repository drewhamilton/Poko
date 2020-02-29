package dev.drewhamilton.careful.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class CarefulGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // TODO: Decide on plugin block name
        target.extensions.create("carefulClasses", CarefulPluginExtension::class.java)
    }
}
