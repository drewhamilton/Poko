import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.objectweb.asm.ClassReader

class JvmPerformanceTest {
    @Test fun `int property does not emit hashCode method invocation`() {
        val classfile = jvmOutput("performance/IntAndLong.class")
        val bytecode = bytecodeToText(classfile.readBytes())
        assertThat(bytecode).all {
            contains("java/lang/Long.hashCode")
            doesNotContain("java/lang/Integer.hashCode")
        }
    }

    @Test fun `uint property does not emit hashCode method invocation`() {
        val classfile = jvmOutput("performance/UIntAndLong.class")
        val bytecode = bytecodeToText(classfile.readBytes())
        assertThat(bytecode).all {
            contains("java/lang/Long.hashCode")
            doesNotContain("kotlin/UInt.hashCode-impl")
        }
    }

    @Test fun `toString uses invokedynamic on modern JDKs`() {
        val classfile = jvmOutput("performance/IntAndLong.class")
        val classReader = ClassReader(classfile.readBytes())
        // Java 9 == class file major version 53:
        classReader.assumeMinimumClassVersion(53)
        val bytecode = classReader.toText()
        assertThat(bytecode).all {
            contains("INVOKEDYNAMIC makeConcatWithConstants")
            doesNotContain("StringBuilder")
        }
    }

    private fun ClassReader.assumeMinimumClassVersion(version: Int) {
        // Class file major version is a two-byte integer at offset 6:
        val actualClassVersion = readShort(6)
        if (actualClassVersion < version) {
            throw AssumptionViolatedException("This test only works class version $version+")
        }
    }
}
