package dev.drewhamilton.extracare

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExtraCarePluginTest {

    @JvmField
    @Rule var temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test fun `compilation of valid class succeeds`() {
        val classFile = SourceFile.kotlin("DataApiClass.kt", VALID_DATA_API_CLASS)

        testWithAndWithoutIr(classFile)
    }

    @Test fun `compilation of data class fails`() {
        val classFile = SourceFile.kotlin("DataClass.kt", VALID_DATA_API_CLASS.replace("class", "data class"))

        testWithAndWithoutIr(classFile, expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR) { result ->
            assertThat(result.messages).contains("@DataApi does not support data classes")
        }
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

        testWithAndWithoutIr(classFile, expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR) { result ->
            assertThat(result.messages).contains("@DataApi classes must have a primary constructor")
        }
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

        testWithAndWithoutIr(classFile)
    }

    @Test fun `compiled Simple class instance has expected toString`() {
        val classFile = SourceFile.fromPath("src/test/resources/Simple.kt")

        testWithAndWithoutIr(classFile) { result ->
            val clazz = result.classLoader.loadClass("Simple")
            val constructor = clazz.getConstructor(Int::class.java, String::class.java, String::class.java)

            val instance = constructor.newInstance(1, "String", null)
            assertThat(instance.toString()).isEqualTo("Simple(int=1, requiredString=String, optionalString=null)")
        }
    }

    private inline fun testWithAndWithoutIr(
        vararg sourceFiles: SourceFile,
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (KotlinCompilation.Result) -> Unit = {}
    ) {
        val nonIrResult = prepareCompilation(false, *sourceFiles).compile()
        assertThat(nonIrResult.exitCode).isEqualTo(expectedExitCode)
        additionalTesting(nonIrResult)

        val irResult = prepareCompilation(true, *sourceFiles).compile()
        assertThat(irResult.exitCode).isEqualTo(expectedExitCode)
        additionalTesting(irResult)
    }

    private fun prepareCompilation(
        useIr: Boolean,
        vararg sourceFiles: SourceFile
    ) = KotlinCompilation().apply {
        workingDir = temporaryFolder.root
        compilerPlugins = listOf<ComponentRegistrar>(ExtraCareComponentRegistrar())
        inheritClassPath = true
        sources = sourceFiles.asList()
        verbose = false
        jvmTarget = JvmTarget.fromString("1.8")!!.description
        useIR = useIr
    }

    private fun SourceFile.Companion.fromPath(path: String): SourceFile = fromPath(File(path))

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
