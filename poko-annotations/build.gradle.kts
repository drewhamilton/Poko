apply(plugin = "org.jetbrains.kotlin.jvm")

extra.apply {
  set("artifactName", project.property("publish_annotations_artifact")!!)
  set("pomName", "Poko Annotations")
}
apply(from = "../publish.gradle")
