package dev.drewhamilton.poko.build

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Generates an `ArtifactInfo` class with information about the Poko artifacts in [basePackage] in the calling Project.
 * Tasks of type [KotlinCompilationTask] will depend on the generation task's provider, and each of [dependentTasks]
 * will depend on the generation task directly.
 */
fun Project.generateArtifactInfo(
    basePackage: String,
) {
    val generateArtifactInfoProvider = tasks.register(
        "generateArtifactInfo",
        Copy::class.java,
        GenerateArtifactInfoAction(this, basePackage),
    )
    generateArtifactInfoProvider.configure {
        from(rootProject.layout.projectDirectory.dir("artifact-info-template"))
        into(layout.buildDirectory.dir("generated/source/artifact-info-template/main"))
    }

    sourceSets {
        getByName("main").java.srcDir(generateArtifactInfoProvider)
    }
}

@PublishedApi
internal class GenerateArtifactInfoAction(
    private val project: Project,
    private val basePackage: String,
) : Action<Copy> {

    override fun execute(t: Copy) {
        t.expand(
            mapOf(
                "basePackage" to basePackage,
                "publishGroup" to "${project.group}",
                "publishVersion" to "${project.version}",
                "annotationsArtifact" to artifactIdForProject("poko-annotations"),
                "compilerPluginArtifact" to artifactIdForProject("poko-compiler-plugin"),
                "gradlePluginArtifact" to artifactIdForProject("poko-gradle-plugin"),
            )
        )
        t.filteringCharset = "UTF-8"
    }

    private fun artifactIdForProject(projectName: String): Any {
        return project.rootProject.project(projectName).property("POM_ARTIFACT_ID")!!
    }
}

@PublishedApi
internal fun Project.`sourceSets`(configure: Action<SourceSetContainer>) {
    extensions.configure("sourceSets", configure)
}
