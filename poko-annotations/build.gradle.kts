import dev.drewhamilton.poko.build.setUpLocalSigning
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
}

kotlin {
  explicitApi = ExplicitApiMode.Strict
}

setUpLocalSigning()

@Suppress("UnstableApiUsage")
mavenPublishing {
  coordinates(
    groupId = project.property("GROUP") as String,
    artifactId = project.property("publishAnnotationsArtifact") as String,
    version = project.property("VERSION_NAME") as String,
  )

  pom {
    name.set("Poko Annotations")
  }
}
