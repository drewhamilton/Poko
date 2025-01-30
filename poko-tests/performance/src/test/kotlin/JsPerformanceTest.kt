
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import org.junit.Test

class JsPerformanceTest {
    @Test fun `equals does not perform redundant instanceof check`() {
        val javascript = jsOutput().readText()

        // Hack to filter out data classes, which do have the `THROW_CCE` code:
        val intAndLongLines = javascript.split("\n").filter { it.contains("IntAndLong") }
        assertAll {
            assertThat(
                actual = intAndLongLines.filter { it.contains("other instanceof IntAndLong") },
            ).hasSize(1)

            assertThat(
                actual = intAndLongLines.filter { it.contains("THROW_CCE") },
            ).isEmpty()
        }
    }
}
