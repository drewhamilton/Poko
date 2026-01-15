plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.poko)
}

kotlin {
    jvm()

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

poko.pokoAnnotation = "com/example/CustomPoko"
