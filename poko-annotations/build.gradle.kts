import dev.drewhamilton.poko.build.setUpPublication

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  `maven-publish`
  signing
}

setUpPublication(
  artifactName = project.property("publishAnnotationsArtifact") as String,
  pomName = "Poko Annotations",
)
