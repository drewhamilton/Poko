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
        val classFile = SourceFile.kotlin("DataApiClass.kt", VALID_DATA_API_CLASS)

        testCompilation(classFile, useIr)
    }

    @Test fun `compilation of data class fails`() {
        `compilation of data class fails`(useIr = false)
    }

    @Test fun `IR compilation of data class fails`() {
        `compilation of data class fails`(useIr = true)
    }

    private fun `compilation of data class fails`(useIr: Boolean) {
        val classFile = SourceFile.kotlin("DataClass.kt", VALID_DATA_API_CLASS.replace("class", "data class"))

        testCompilation(classFile, useIr, KotlinCompilation.ExitCode.COMPILATION_ERROR) { result ->
            assertThat(result.messages).contains("@DataApi does not support data classes")
        }
    }

    @Test fun `compilation without primary constructor fails`() {
        `compilation without primary constructor fails`(useIr = false)
    }

    @Test fun `IR compilation without primary constructor fails`() {
        `compilation without primary constructor fails`(useIr = true)
    }

    private fun `compilation without primary constructor fails`(useIr: Boolean) {
        val classWithoutPrimaryConstructor = """
            package dev.drewhamilton.extracare

            import dev.drewhamilton.extracare.DataApi

            @DataApi class DataApiClass {
                constructor(string: String)
            }
        """.trimIndent()
        val classFile = SourceFile.kotlin("SecondaryConstructorClass.kt", classWithoutPrimaryConstructor)

        testCompilation(classFile, useIr, KotlinCompilation.ExitCode.COMPILATION_ERROR) { result ->
            assertThat(result.messages).contains("@DataApi classes must have a primary constructor")
        }
    }

    @Test fun `compilation with explicit function declarations succeeds`() {
        `compilation with explicit function declarations succeeds`(useIr = false)
    }

    @Test fun `IR compilation with explicit function declarations succeeds`() {
        `compilation with explicit function declarations succeeds`(useIr = true)
    }

    private fun `compilation with explicit function declarations succeeds`(useIr: Boolean) {
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

        testCompilation(classFile, useIr)
    }

    @Test fun `compiled Simple class instance has expected toString`() {
        `compiled Simple class instance has expected toString`(useIr = false)
    }

    @Test fun `IR-compiled Simple class instance has expected toString`() {
        `compiled Simple class instance has expected toString`(useIr = true)
    }

    private fun `compiled Simple class instance has expected toString`(useIr: Boolean) {
        val className = "Simple"
        testCompilation(className, useIr) { result ->
            val clazz = result.classLoader.loadClass(className)
            val constructor = clazz.getConstructor(Int::class.java, String::class.java, String::class.java)

            val instance = constructor.newInstance(1, "String", null)
            assertThat(instance.toString()).isEqualTo("$className(int=1, requiredString=String, optionalString=null)")
        }
    }

    @Test fun `compiled Complex class instance has expected toString`() {
        `compiled Complex class instance has expected toString`(useIr = false)
    }

    @Test fun `IR-compiled Complex class instance has expected toString`() {
        `compiled Complex class instance has expected toString`(useIr = true)
    }

    private fun `compiled Complex class instance has expected toString`(useIr: Boolean) {
        val className = "Complex"
        testCompilation(className, useIr) { result ->
            val clazz = result.classLoader.loadClass(className)
            val constructor = clazz.getConstructor(
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaObjectType,
                Long::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Double::class.javaPrimitiveType,
                Array<String>::class.java,
                Array<String>::class.java,
                IntArray::class.java,
                IntArray::class.java,
                List::class.java,
                List::class.java,
                Any::class.java,
                Any::class.java,
            )

            val instance = constructor.newInstance(
                "Text", null,
                2, null,
                12345L, 67f, 89.0,
                arrayOf("Strings"), null,
                intArrayOf(3, 4, 5), null,
                listOf(6, 7, 8), null,
                9, null
            )
            assertThat(instance.toString()).isEqualTo(
                "$className(" +
                        "referenceType=Text, " +
                        "nullableReferenceType=null, " +
                        "int=2, " +
                        "nullableInt=null, " +
                        "long=12345, " +
                        "float=67.0, " +
                        "double=89.0, " +
                        "arrayReferenceType=[Strings], " +
                        "nullableArrayReferenceType=null, " +
                        "arrayPrimitiveType=[3, 4, 5], " +
                        "nullableArrayPrimitiveType=null, " +
                        "genericCollectionType=[6, 7, 8], " +
                        "nullableGenericCollectionType=null, " +
                        "genericType=9, " +
                        "nullableGenericType=null" +
                        ")"
            )
        }
    }

    //region Helpers for all tests
    private inline fun testCompilation(
        sourceFileName: String,
        useIr: Boolean = false,
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (KotlinCompilation.Result) -> Unit = {}
    ) = testCompilation(
        SourceFile.fromPath("src/test/resources/$sourceFileName.kt"),
        useIr,
        expectedExitCode,
        additionalTesting
    )

    private inline fun testCompilation(
        sourceFile: SourceFile,
        useIr: Boolean = false,
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (KotlinCompilation.Result) -> Unit = {}
    ) {
        val result = prepareCompilation(useIr, sourceFile).compile()
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

    private companion object {
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
