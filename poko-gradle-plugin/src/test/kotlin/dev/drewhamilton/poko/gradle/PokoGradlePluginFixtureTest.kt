package dev.drewhamilton.poko.gradle

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import dev.drewhamilton.poko.gradle.TestBuildConfig.MINIMUM_GRADLE_VERSION
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PokoGradlePluginFixtureTest(
    @param:TestParameter(LATEST_GRADLE_VERSION, MINIMUM_GRADLE_VERSION)
    private val gradleVersion: String,
    @param:TestParameter
    private val isolatedProjects: Boolean,
) {
    @Test fun simple() {
        createRunner(File("src/test/fixtures/simple")).build()
    }

    private fun createRunner(
        fixtureDir: File,
        vararg tasks: String = arrayOf("clean", "build")
    ): GradleRunner {
        return GradleRunner.create()
            .apply {
                if (gradleVersion != LATEST_GRADLE_VERSION) {
                    withGradleVersion(gradleVersion)
                }
            }
            .withProjectDir(fixtureDir)
            .withDebug(true) // Run in-process.
            .withArguments(
                *tasks,
                "--stacktrace",
                VERSION_PROPERTY,
                VALIDATE_KOTLIN_METADATA,
                "-Dorg.gradle.configuration-cache=true",
                "-Dorg.gradle.unsafe.isolated-projects=$isolatedProjects"
            )
            .forwardOutput()
    }
}

private const val LATEST_GRADLE_VERSION = "latest"

private const val VERSION_PROPERTY = "-PpokoVersion=${BuildConfig.VERSION}"
private const val VALIDATE_KOTLIN_METADATA = "-Porg.gradle.kotlin.dsl.skipMetadataVersionCheck=false"
