package dev.drewhamilton.poko.build

import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.publishing
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

/**
 * Set up the project for publication to Maven Central. Project must have applied Dokka, `maven-publish`, and `signing`
 * plugins. Its source code must be in `src/main/kotlin`.
 *
 * [gradlePluginDomainObjectName] need only be provided if the Project is a Gradle plugin.
 */
fun Project.setUpPublication(
    artifactName: String,
    pomName: String,
    gradlePluginDomainObjectName: String? = null,
) {
    group = rootProject.property("publishGroup") as String
    version = rootProject.property("publishVersion") as String

    setUpArchiveJars()
    setUpMavenPublication(artifactName, pomName, gradlePluginDomainObjectName)
    setUpSigning()
}

private fun Project.setUpArchiveJars() {
    tasks.named("dokkaJavadoc").configure {
        inputs.dir("src/main/kotlin")
    }

    extensions.configure<JavaPluginExtension>("java") {
        withJavadocJar()
        withSourcesJar()
    }
    tasks.named<Jar>("javadocJar") {
        from(tasks.named("dokkaJavadoc"))
    }

    tasks.named("assemble").configure {
        dependsOn("sourcesJar")
        dependsOn("javadocJar")
    }
}

private fun Project.setUpMavenPublication(
    artifactName: String,
    pomName: String,
    gradlePluginDomainObjectName: String?,
) {
    extensions.configure<PublishingExtension>("publishing") {
        val isGradlePlugin = project.plugins.hasPlugin("java-gradle-plugin")

        val publicationNames = mutableListOf<String>()
        if (isGradlePlugin) {
            publicationNames.add("pluginMaven")
            publicationNames.add("${gradlePluginDomainObjectName!!}PluginMarkerMaven")
        } else {
            publicationNames.add("release")
        }

        publicationNames.forEach { publicationName ->
            if (!publicationName.contains("PluginMarkerMaven")) {
                publications.create<MavenPublication>(publicationName) {
                    if (!isGradlePlugin) {
                        groupId = project.group as String
                        artifactId = artifactName
                        version = project.version as String

                        from(components["java"])
                    }
                }
            }

            // Using afterEvaluate because Plugin Marker publication is created later, by a different plugin:
            afterEvaluate {
                val publication = publications.getByName(publicationName) as MavenPublication
                publication.applyPokoPom(pomName)
            }
        }

        repositories {
            maven {
                name = "MavenCentral"

                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
                url = if ((version as String).endsWith("SNAPSHOT")) {
                    uri(snapshotsRepoUrl)
                } else {
                    uri(releasesRepoUrl)
                }

                val sonatypeIssuesUsername = findProperty("personalSonatypeIssuesUsername") as String?
                val sonatypeIssuesPassword = findProperty("personalSonatypeIssuesPassword") as String?
                credentials {
                    username = sonatypeIssuesUsername
                    password = sonatypeIssuesPassword
                }
            }
        }
    }
}

private fun MavenPublication.applyPokoPom(pomName: String) = pom {
    name.set(pomName)
    description.set(
        "A Kotlin compiler plugin for generating equals, hashCode, and toString for plain old Kotlin objects.",
    )

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

@PublishedApi
internal fun Project.setUpSigning() {
    val isCi = System.getenv()["CI"] == "true"
    if (!isCi) {
        extra["signing.keyId"] = findProperty("personalGpgKeyId") ?: "x"
        extra["signing.password"] = findProperty("personalGpgPassword") ?: "x"
        extra["signing.secretKeyRingFile"] = findProperty("personalGpgKeyringFile") ?: "x"
    }
    extensions.configure<SigningExtension>("signing") {
        if (isCi) {
            logger.lifecycle("Signing on CI")
            val key = findProperty("personalGpgKey") as String?
            val password = findProperty("personalGpgPassword") as String?
            useInMemoryPgpKeys(key, password)
        }
        sign(publishing.publications)
    }
}

private val Project.publishing: PublishingExtension
    get() = extensions.getByName("publishing") as PublishingExtension
