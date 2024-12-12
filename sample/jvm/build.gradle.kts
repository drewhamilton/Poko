plugins {
    alias(libs.plugins.kotlin.jvm)
    id("dev.drewhamilton.poko")
    `java-test-fixtures`
}

poko {
    pokoAnnotation = "dev/drewhamilton/poko/sample/jvm/MyData"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}
