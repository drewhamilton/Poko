import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove in Gradle 8.1
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    id("dev.drewhamilton.poko") apply false
}

val resolvedJavaVersion = when (val ciJavaVersion = System.getenv()["ci_java_version"]) {
    null -> JavaVersion.VERSION_11
    "8", "9", "10" -> JavaVersion.valueOf("VERSION_1_$ciJavaVersion")
    else -> JavaVersion.valueOf("VERSION_$ciJavaVersion")
}
logger.lifecycle("Targeting Java version $resolvedJavaVersion")

val kotlinJvmTarget = when (resolvedJavaVersion) {
    JavaVersion.VERSION_1_8 -> JvmTarget.JVM_1_8
    else -> JvmTarget.valueOf("JVM_${resolvedJavaVersion.majorVersion}")
}

allprojects {
    repositories {
        if (System.getenv()["CI"] == "true") {
            logger.lifecycle("Resolving ${this@allprojects} Poko dependencies from MavenLocal")
            exclusiveContent {
                forRepository { mavenLocal() }
                filter {
                    @Suppress("LocalVariableName") val publish_group: String by extra
                    includeGroup(publish_group)
                }
            }
        }
        mavenCentral()
    }

    plugins.withId("com.android.library") {
        with(extensions.getByType<LibraryExtension>()) {
            compileOptions {
                sourceCompatibility(resolvedJavaVersion)
                targetCompatibility(resolvedJavaVersion)
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        with(extensions.getByType<JavaPluginExtension>()) {
            sourceCompatibility = resolvedJavaVersion
            targetCompatibility = resolvedJavaVersion
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(kotlinJvmTarget)
            freeCompilerArgs.add("-progressive")
        }
    }
}
