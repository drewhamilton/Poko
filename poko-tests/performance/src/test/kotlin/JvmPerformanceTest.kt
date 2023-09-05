import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import org.junit.Test

class JvmPerformanceTest {
    @Test fun `int property does not emit hashCode method invocation`() {
        val classfile = jvmOutput("performance/IntAndLong.class")
        val bytecode = bytecodeToText(classfile.readBytes())
        assertThat(bytecode).all {
            contains("java/lang/Long.hashCode")
            doesNotContain("java/lang/Integer.hashCode")
        }
    }
}
