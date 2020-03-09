package dev.drewhamilton.extracare.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class ExtraCareGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("extraCare", ExtraCarePluginExtension::class.java)
    }
}
