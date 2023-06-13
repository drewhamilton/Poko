import dev.drewhamilton.poko.build.setUpPublication
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  `maven-publish`
  signing
}

kotlin {
  explicitApi = ExplicitApiMode.Strict
}

setUpPublication(
  artifactName = project.property("publishAnnotationsArtifact") as String,
  pomName = "Poko Annotations",
)
