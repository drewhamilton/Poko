package dev.drewhamilton.extracare.sample.explicit

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Objects

class ChildOfExplicitFunctionDeclarationsTest {

    private val childOfExplicitFunctionDeclarations = ChildOfExplicitFunctionDeclarations("test")

    @Test fun `ExplicitFunctionDeclarations overrides explicit function declarations`() {
        assertThat(childOfExplicitFunctionDeclarations.toString())
            .isEqualTo("ChildOfExplicitFunctionDeclarations(string=test)")
        assertThat(childOfExplicitFunctionDeclarations).isNotEqualTo(true)
        assertThat(childOfExplicitFunctionDeclarations).isNotEqualTo(false)
        assertThat(childOfExplicitFunctionDeclarations).isEqualTo(ChildOfExplicitFunctionDeclarations("test"))
        assertThat(childOfExplicitFunctionDeclarations.hashCode()).isEqualTo(Objects.hash("test") - 31)
    }
}
