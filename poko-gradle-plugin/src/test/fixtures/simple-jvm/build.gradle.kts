import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.poko)
    id("java-test-fixtures")
    id("jvm-test-suite")
}

testing.suites {
    named("test", JvmTestSuite::class) {
        useJUnit()
        dependencies {
            implementation(testFixtures(project()))
        }
    }

    register("integrationTest", JvmTestSuite::class) {
        useJUnit()
        dependencies {
            implementation(testFixtures(project()))
        }
        targets.named(name) {
            tasks.check {
                dependsOn(testTask)
            }
        }
    }
}
