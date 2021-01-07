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
        `compilation of valid class succeeds`(useIr = false)
    }

    @Test fun `IR compilation of valid class succeeds`() {
        `compilation of valid class succeeds`(useIr = true)
    }

    private fun `compilation of valid class succeeds`(useIr: Boolean) {
        testCompilation("api/Primitives", useIr = useIr)
    }

    @Test fun `compilation of data class fails`() {
        `compilation of data class fails`(useIr = false)
    }

    @Test fun `IR compilation of data class fails`() {
        `compilation of data class fails`(useIr = true)
    }

    private fun `compilation of data class fails`(useIr: Boolean) {
        testCompilation(
            "illegal/Data",
            useIr = useIr,
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages).contains("@DataApi does not support data classes")
        }
    }

    @Test fun `compilation of inline class fails`() {
        `compilation of inline class fails`(useIr = false)
    }

    @Test fun `IR compilation of inline class fails`() {
        `compilation of inline class fails`(useIr = true)
    }

    private fun `compilation of inline class fails`(useIr: Boolean) {
        testCompilation(
            "illegal/Inline",
            useIr = useIr,
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages).contains("@DataApi does not support inline classes")
        }
    }

    @Test fun `compilation without primary constructor fails`() {
        `compilation without primary constructor fails`(useIr = false)
    }

    @Test fun `IR compilation without primary constructor fails`() {
        `compilation without primary constructor fails`(useIr = true)
    }

    private fun `compilation without primary constructor fails`(useIr: Boolean) {
        testCompilation(
            "illegal/NoPrimaryConstructor",
            useIr = useIr,
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages).contains("@DataApi classes must have a primary constructor")
        }
    }

    @Test fun `compilation with explicit function declarations respects explicit toString`() {
        `compilation with explicit function declarations respects explicit toString`(useIr = false)
    }

    @Test fun `IR compilation with explicit function declarations respects explicit toString`() {
        `compilation with explicit function declarations respects explicit toString`(useIr = true)
    }

    private fun `compilation with explicit function declarations respects explicit toString`(useIr: Boolean) {
        val testString = "test string"
        compareWithDataClass(
            "ExplicitDeclarations",
            listOf(String::class.java to testString),
            useIr
        ) { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(testString)
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }
    }

    @Test fun `compiled Simple class instance has expected toString`() {
        `compiled Simple class instance has expected toString`(useIr = false)
    }

    @Test fun `IR-compiled Simple class instance has expected toString`() {
        `compiled Simple class instance has expected toString`(useIr = true)
    }

    private fun `compiled Simple class instance has expected toString`(useIr: Boolean) {
        compareWithDataClass(
            "Simple",
            listOf(Int::class.java to 1, String::class.java to "String", String::class.java to null),
            useIr
        ) { apiClass, dataClass ->
            assertThat(apiClass.toString()).isEqualTo(dataClass.toString())
        }
    }

    @Test fun `compiled Complex class instance has expected toString`() {
        `compiled Complex class instance has expected toString`(useIr = false)
    }

    @Test fun `IR-compiled Complex class instance has expected toString`() {
        `compiled Complex class instance has expected toString`(useIr = true)
    }

    private fun `compiled Complex class instance has expected toString`(useIr: Boolean) {
        compareWithDataClass(
            "Complex",
            listOf(
                String::class.java to "Text",
                String::class.java to null,
                Int::class.javaPrimitiveType!! to 2,
                Int::class.javaObjectType to null,
                Long::class.javaPrimitiveType!! to 12345L,
                Float::class.javaPrimitiveType!! to 67f,
                Double::class.javaPrimitiveType!! to 89.0,
                Array<String>::class.java to arrayOf("Strings"),
                Array<String>::class.java to null,
                IntArray::class.java to intArrayOf(3, 4, 5),
                IntArray::class.java to null,
                List::class.java to listOf(6, 7, 8),
                List::class.java to null,
                Any::class.java to 9,
                Any::class.java to null,
            ),
            useIr
        ) { apiClass, dataClass ->
            assertThat(apiClass.toString()).isEqualTo(dataClass.toString())
        }
    }

    //region Helpers for all tests
    /**
     * Compiles and instantiates [sourceFileName] from both the `api` package and the `data` package, with the
     * expectation that they compile to a [DataApi] class and a data class, respectively. After instantiating each,
     * passes both to [compare] for comparison testing.
     */
    private inline fun compareWithDataClass(
        sourceFileName: String,
        constructorArgs: List<Pair<Class<*>, Any?>>,
        useIr: Boolean = false,
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = testCompilation("api/$sourceFileName", "data/$sourceFileName", useIr = useIr) { result ->
        val apiClass = result.classLoader.loadClass("api.$sourceFileName")
        val dataClass = result.classLoader.loadClass("api.$sourceFileName")

        val constructorArgParameterTypes = constructorArgs.map { it.first }.toTypedArray()
        val apiConstructor = apiClass.getConstructor(*constructorArgParameterTypes)
        val dataConstructor = dataClass.getConstructor(*constructorArgParameterTypes)

        val constructorParameters = constructorArgs.map { it.second }.toTypedArray()
        val apiInstance = apiConstructor.newInstance(*constructorParameters)
        val dataInstance = dataConstructor.newInstance(*constructorParameters)

        compare(apiInstance, dataInstance)
    }

    private inline fun testCompilation(
        vararg sourceFileNames: String,
        useIr: Boolean = false,
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (KotlinCompilation.Result) -> Unit = {}
    ) = testCompilation(
        *sourceFileNames.map { SourceFile.fromPath("src/test/resources/$it.kt") }.toTypedArray(),
        useIr = useIr,
        expectedExitCode = expectedExitCode,
        additionalTesting = additionalTesting
    )

    private inline fun testCompilation(
        vararg sourceFiles: SourceFile,
        useIr: Boolean = false,
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (KotlinCompilation.Result) -> Unit = {}
    ) {
        val result = prepareCompilation(useIr, *sourceFiles).compile()
        assertThat(result.exitCode).isEqualTo(expectedExitCode)
        additionalTesting(result)
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
    //endregion
}
