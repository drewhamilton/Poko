package dev.drewhamilton.extracare.sample.explicit

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExplicitFunctionDeclarationsTest {

    private val explicitFunctionDeclarations = ExplicitFunctionDeclarations("test")

    @Test fun `ExplicitFunctionDeclarations honors explicit function declarations`() {
        assertThat(explicitFunctionDeclarations.toString()).isEqualTo("test")
        assertThat(explicitFunctionDeclarations).isEqualTo(true)
        assertThat(explicitFunctionDeclarations).isNotEqualTo(false)
        assertThat(explicitFunctionDeclarations.hashCode()).isEqualTo("test".hashCode())
    }
}
