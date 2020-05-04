package dev.drewhamilton.extracare

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExtraCarePluginTest {

    @JvmField
    @Rule var temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test fun `compilation of valid class succeeds`() {
        val classFile = SourceFile.kotlin("DataApiClass.kt", VALID_DATA_API_CLASS)
        val result = prepareCompilation(classFile).compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test fun `compilation of data class fails`() {
        val classFile = SourceFile.kotlin("DataClass.kt", VALID_DATA_API_CLASS.replace("class", "data class"))
        val result = prepareCompilation(classFile).compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("@DataApi does not support data classes")
    }

    @Test fun `compilation without primary constructor fails`() {
        val classWithoutPrimaryConstructor = """
            package dev.drewhamilton.extracare

            import dev.drewhamilton.extracare.DataApi

            @DataApi class DataApiClass {
                constructor(string: String)
            }
        """.trimIndent()
        val classFile = SourceFile.kotlin("SecondaryConstructorClass.kt", classWithoutPrimaryConstructor)
        val result = prepareCompilation(classFile).compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("@DataApi classes must have a primary constructor")
    }

    @Test fun `compilation with explicit function declarations succeeds`() {
        val classWithExplicitFunctionDeclarations = """
            package dev.drewhamilton.extracare

            import dev.drewhamilton.extracare.DataApi

            @DataApi class ExplicitDeclarationsClass(private val string: String) {
                override fun toString() = string
                override fun equals(other: Any?) = other == string
                override fun hashCode() = string.hashCode()
            }
        """.trimIndent()
        val classFile = SourceFile.kotlin("ExplicitFunctionDeclarationClass.kt", classWithExplicitFunctionDeclarations)
        val result = prepareCompilation(classFile).compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    private fun prepareCompilation(vararg sourceFiles: SourceFile) = KotlinCompilation().apply {
        workingDir = temporaryFolder.root
        compilerPlugins = listOf<ComponentRegistrar>(ExtraCareComponentRegistrar())
        inheritClassPath = true
        sources = sourceFiles.asList()
        verbose = false
        jvmTarget = JvmTarget.fromString("1.8")!!.description
    }

    companion object {
        private val VALID_DATA_API_CLASS = """
            package dev.drewhamilton.extracare

            import dev.drewhamilton.extracare.DataApi

            @DataApi class DataApiClass(
                val string: String,
                val float: Float,
                val double: Double,
                val long: Long,
                val int: Int,
                val short: Short,
                val byte: Byte,
                val boolean: Boolean
            )
        """.trimIndent()
    }
}
