import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import org.junit.AssumptionViolatedException
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
        val bytecode = bytecodeToText(classfile.readBytes()).also {
            // Class version 55 == Java 11
            it.assumeMinimumClassVersion(55)
        }

        assertThat(bytecode).all {
            contains("INVOKEDYNAMIC makeConcatWithConstants")
            doesNotContain("StringBuilder")
        }
    }

    private fun String.assumeMinimumClassVersion(version: Int) {
        val classVersionRegex = Regex("class version [\\d.]* \\((\\d*)\\)")
        val actualClassVersion = classVersionRegex.find(this)!!.groups.last()!!.value.toInt()
        if (actualClassVersion < version) {
            throw AssumptionViolatedException("This test only works class version $version+")
        }
    }
}
