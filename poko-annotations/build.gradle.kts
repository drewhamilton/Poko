import org.jetbrains.dokka.gradle.DokkaTask

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  `maven-publish`
  signing
}

extra.apply {
  set("artifactName", project.property("publishAnnotationsArtifact")!!)
  set("pomName", "Poko Annotations")
}
//apply(from = "../publish.gradle")

group = rootProject.property("publishGroup") as String
version = rootProject.property("publishVersion") as String

tasks.register<Jar>("sourcesJar") {
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

//apply(plugin = "org.jetbrains.dokka")
tasks.withType<DokkaTask>().configureEach {
  inputs.dir("src/main/kotlin")
}

java {
  withJavadocJar()
  withSourcesJar()
}
tasks.named<Jar>("javadocJar") {
  from(tasks.named<DokkaTask>("dokkaJavadoc"))
}

tasks.named("assemble").configure {
  dependsOn("sourcesJar")
  dependsOn("javadocJar")
}

//apply(plugin = "maven-publish")
publishing {
  val isGradlePlugin = project.plugins.hasPlugin("java-gradle-plugin")

  val publicationNames = mutableListOf<String>()
  if (isGradlePlugin) {
    publicationNames.add("pluginMaven")
    val gradlePluginDomainObjectName: String by extra
    publicationNames.add("${gradlePluginDomainObjectName}PluginMarkerMaven")
  } else {
    publicationNames.add("release")
  }

  publicationNames.forEach { publicationName ->
    if (!publicationName.contains("PluginMarkerMaven")) {
      publications.create<MavenPublication>(publicationName) {
        if (!isGradlePlugin) {
          groupId = project.group as String
          artifactId = project.extra["artifactName"] as String
          version = project.version as String

          from(components["java"])
        }
      }
    }

    // Using afterEvaluate because Plugin Marker publication is created later, by a different plugin:
    afterEvaluate {
      val publication = publications.getByName(publicationName) as MavenPublication
      publication.pom {
        name.set(property("pomName") as String)
        description.set("A Kotlin compiler plugin for generating equals, hashCode, and toString for plain old Kotlin objects.")

        url.set("https://github.com/drewhamilton/Poko")
        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }

        developers {
          developer {
            id.set("drewhamilton")
            name.set("Drew Hamilton")
            email.set("software@drewhamilton.dev")
          }
        }

        scm {
          connection.set("scm:git:github.com/drewhamilton/Poko.git")
          developerConnection.set("scm:git:ssh://github.com/drewhamilton/Poko.git")
          url.set("https://github.com/drewhamilton/Poko/tree/main")
        }
      }
    }
  }

  repositories {
    maven {
      name = "MavenCentral"

      val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
      val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
      url = if ((version as String).endsWith("SNAPSHOT")) uri(snapshotsRepoUrl) else uri(releasesRepoUrl)

      val sonatypeIssuesUsername = findProperty("personalSonatypeIssuesUsername") as String?
      val sonatypeIssuesPassword = findProperty("personalSonatypeIssuesPassword") as String?
      credentials {
        username = sonatypeIssuesUsername
        password = sonatypeIssuesPassword
      }
    }
  }
}

//apply(plugin = "signing")
val isCi = System.getenv()["CI"] == "true"
if (!isCi) {
  extra["signing.keyId"] = findProperty("personalGpgKeyId") ?: "x"
  extra["signing.password"] = findProperty("personalGpgPassword") ?: "x"
  extra["signing.secretKeyRingFile"] = findProperty("personalGpgKeyringFile") ?: "x"
}
signing {
  if (isCi) {
    logger.lifecycle("Signing on CI")
    val key = findProperty("personalGpgKey") as String?
    val password = findProperty("personalGpgPassword") as String?
    useInMemoryPgpKeys(key, password)
  }
  sign(publishing.publications)
}
