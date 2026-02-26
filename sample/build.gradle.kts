import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    id("dev.drewhamilton.poko") apply false
}
apply(from = "properties.gradle")

private val javaVersion = JavaVersion.VERSION_11
private val kotlinJvmTarget: JvmTarget = when (javaVersion) {
    JavaVersion.VERSION_1_8 -> JvmTarget.JVM_1_8
    else -> JvmTarget.valueOf("JVM_${javaVersion.majorVersion}")
}

logger.lifecycle("Compiling sample app with Kotlin ${libs.versions.kotlin.get()}")
logger.lifecycle("Targeting Java version $javaVersion")

val specifiedKotlinLanguageVersion = findProperty("pokoSample_kotlinLanguageVersion")
    ?.toString()
    ?.let { it.ifBlank { null } }
    ?.let { KotlinVersion.fromVersion(it) }
    ?.also { logger.lifecycle("Compiling sample project with language version $it") }

allprojects {
    repositories {
        if (System.getenv()["CI"] == "true") {
            logger.lifecycle("Resolving ${this@allprojects} Poko dependencies from MavenLocal")
            exclusiveContent {
                forRepository { mavenLocal() }
                filter {
                    includeGroup(property("PUBLISH_GROUP") as String)
                }
            }
        }

        mavenCentral()

        // Used for non-ephemeral dev versions:
        maven("https://redirector.kotlinlang.org/maven/bootstrap")

        val kotlinDevRepository = rootProject.findProperty("kotlin_dev_repository")
        if (kotlinDevRepository != null) {
            logger.lifecycle("Adding <$kotlinDevRepository> repository for ${this@allprojects}")
            maven { url = uri(kotlinDevRepository) }
        }
    }

    listOf(
        "org.jetbrains.kotlin.jvm",
        "org.jetbrains.kotlin.multiplatform",
    ).forEach { id ->
        plugins.withId(id) {
            with(extensions.getByType<JavaPluginExtension>()) {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
        }
    }

    plugins.withId("com.android.library") {
        with(extensions.getByType<CommonExtension>()) {
            with(compileOptions) {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
        }
    }

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            if (this is KotlinJvmCompilerOptions) {
                jvmTarget = kotlinJvmTarget
            }
            languageVersion = specifiedKotlinLanguageVersion
            progressiveMode = (specifiedKotlinLanguageVersion == null)
        }
    }
}
