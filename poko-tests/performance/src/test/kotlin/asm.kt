import java.io.PrintWriter
import java.io.StringWriter
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceClassVisitor

fun bytecodeToText(bytecode: ByteArray): String {
    val textifier = Textifier()
    ClassReader(bytecode).accept(TraceClassVisitor(null, textifier, null), 0)

    val writer = StringWriter()
    textifier.print(PrintWriter(writer))
    return writer.toString().trim()
}
