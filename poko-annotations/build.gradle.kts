import dev.drewhamilton.poko.build.setUpPublication

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
