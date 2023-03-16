package dev.drewhamilton.poko.build

import kotlin.reflect.KClass
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer

/**
 * Generates an `ArtifactInfo` class in [basePackage] in the Project that applies this.
 */
inline fun <reified KotlinCompile : Task> Project.generateArtifactInfo(
    basePackage: String,
    vararg dependentTasks: KClass<out Task>,
) {
    val taskName = "generateArtifactInfo"
    val genDir: String = "generated/source/artifact-info-template/main"
    val generateArtifactInfoProvider = tasks.register(
        taskName,
        Copy::class.java,
        GenerateArtifactInfoAction(this, genDir, basePackage),
    )

    sourceSets {
        getByName("main").java.srcDir("$buildDir/$genDir")
    }

    tasks.withType(KotlinCompile::class.java).configureEach {
        dependsOn(generateArtifactInfoProvider)
    }

    dependentTasks.forEach { taskType ->
        tasks.withType(taskType.java).configureEach {
            dependsOn(taskName)
        }
    }
}

@PublishedApi
internal class GenerateArtifactInfoAction(
    private val project: Project,
    private val genDir: String,
    private val basePackage: String,
) : Action<Copy> {

    override fun execute(t: Copy) {
        t.from(project.rootProject.layout.projectDirectory.dir("artifact-info-template"))
        t.into(project.layout.buildDirectory.dir(genDir))
        t.expand(
            mapOf(
                "basePackage" to basePackage,
                "publishGroup" to "${project.group}",
                "publishVersion" to "${project.version}",
                "annotationsArtifact" to project.property("publishAnnotationsArtifact"),
                "compilerPluginArtifact" to project.property("publishCompilerPluginArtifact"),
                "gradlePluginArtifact" to project.property("publishGradlePluginArtifact"),
            )
        )
        t.filteringCharset = "UTF-8"
    }
}

@PublishedApi
internal fun Project.`sourceSets`(configure: Action<SourceSetContainer>) {
    extensions.configure("sourceSets", configure)
}
