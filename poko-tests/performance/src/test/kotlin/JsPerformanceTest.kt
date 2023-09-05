import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import org.junit.Test

class JsPerformanceTest {
    @Test fun `equals does not perform redundant instanceof check`() {
        val javascript = jsOutput().readText()
        assertThat(javascript).all {
            contains("other instanceof IntAndLong")
            doesNotContain("THROW_CCE")
        }
    }
}
