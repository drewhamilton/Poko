import java.io.File

fun jvmOutput(relativePath: String) = File("../build/classes/kotlin/jvm/main", relativePath)
fun jsOutput() = File("../build/compileSync/js/main/productionExecutable/kotlin/Poko-poko-tests.js")
