@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
  alias(libs.plugins.kotlin.jvm)
}

extra.apply {
  set("artifactName", project.property("publish_annotations_artifact")!!)
  set("pomName", "Poko Annotations")
}
apply(from = "../publish.gradle")
