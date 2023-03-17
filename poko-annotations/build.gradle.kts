@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
  alias(libs.plugins.kotlin.jvm)
}

extra.apply {
  set("artifactName", project.property("publishAnnotationsArtifact")!!)
  set("pomName", "Poko Annotations")
}
apply(from = "../publish.gradle")
