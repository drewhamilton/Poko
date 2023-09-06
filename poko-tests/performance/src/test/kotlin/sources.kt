import java.io.File

fun jvmOutput(relativePath: String, version: Int = -1): File {
    val number = if (version == -1) "" else "$version"
    return File("../build/classes/kotlin/jvm$number/main", relativePath)
}
fun jsOutput() = File("../build/compileSync/js/main/productionExecutable/kotlin/Poko-poko-tests.js")
