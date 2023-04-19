plugins {
    alias(libs.plugins.kotlin.jvm)
    id("dev.drewhamilton.poko")
}

poko {
    pokoAnnotation.set("dev/drewhamilton/poko/sample/jvm/Poko")
    enabled.set(true)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
