package dev.drewhamilton.extracare.sample.explicit

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChildOfExplicitFunctionDeclarationsTest {

    private val childOfExplicitFunctionDeclarations = ChildOfExplicitFunctionDeclarations("test")

    @Test fun `ExplicitFunctionDeclarations overrides explicit function declarations`() {
        assertThat(childOfExplicitFunctionDeclarations.toString()).isEqualTo("test")
        assertThat(childOfExplicitFunctionDeclarations).isEqualTo(true)
        assertThat(childOfExplicitFunctionDeclarations).isNotEqualTo(false)
        assertThat(childOfExplicitFunctionDeclarations.hashCode()).isEqualTo("test".hashCode())
    }
}
