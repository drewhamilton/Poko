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
import java.math.BigDecimal

class ExtraCarePluginTest {

    @JvmField
    @Rule var temporaryFolder: TemporaryFolder = TemporaryFolder()

    //region Primitives
    @Test fun `compilation of valid class succeeds`() {
        `compilation of valid class succeeds`(useIr = false)
    }

    @Test fun `IR compilation of valid class succeeds`() {
        `compilation of valid class succeeds`(useIr = true)
    }

    private fun `compilation of valid class succeeds`(useIr: Boolean) {
        testCompilation("api/Primitives", useIr = useIr)
    }
    //endregion

    //region data class
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
    //endregion

    //region inline class
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
    //endregion

    //region No primary constructor
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
    //endregion

    //region Explicit function declarations
    @Test fun `compilation with explicit function declarations respects explicit hashCode`() {
        `compilation with explicit function declarations respects explicit hashCode`(useIr = false)
    }

    @Test fun `IR compilation with explicit function declarations respects explicit hashCode`() {
        `compilation with explicit function declarations respects explicit hashCode`(useIr = true)
    }

    private fun `compilation with explicit function declarations respects explicit hashCode`(useIr: Boolean) {
        val testString = "test thing"
        compareExplicitDeclarationsInstances(useIr, string = testString) { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(testString.length)
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
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
        compareExplicitDeclarationsInstances(useIr, string = testString) { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(testString)
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }
    }

    private fun compareExplicitDeclarationsInstances(
        useIr: Boolean,
        string: String = "test string",
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = compareWithDataClass(
        sourceFileName = "ExplicitDeclarations",
        constructorArgs = listOf(String::class.java to string),
        useIr = useIr,
        compare = compare
    )
    //endregion

    //region Superclass function declarations
    @Test fun `superclass hashCode is overridden`() {
        `superclass hashCode is overridden`(useIr = false)
    }

    @Test fun `IR superclass hashCode is overridden`() {
        `superclass hashCode is overridden`(useIr = true)
    }

    private fun `superclass hashCode is overridden`(useIr: Boolean) =
        compareSubclassInstances(useIr) { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
            assertThat(apiInstance.hashCode()).isNotEqualTo(50934)
        }

    @Test fun `superclass toString is overridden`() {
        `superclass toString is overridden`(useIr = false)
    }

    @Test fun `IR superclass toString is overridden`() {
        `superclass toString is overridden`(useIr = true)
    }

    private fun `superclass toString is overridden`(useIr: Boolean) =
        compareSubclassInstances(useIr) { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
            assertThat(apiInstance.toString()).isNotEqualTo("superclass")
        }

    private fun compareSubclassInstances(
        useIr: Boolean,
        number: Number = 123.4,
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = compareWithDataClass(
        sourceFileName = "Sub",
        constructorArgs = listOf(Number::class.java to number),
        otherFilesToCompile = listOf("Super"),
        useIr = useIr,
        compare = compare
    )
    //endregion

    //region Nested
    @Test fun `compilation of nested class within class matches corresponding data class toString`() {
        `compilation of nested class within class matches corresponding data class toString`(useIr = false)
    }

    @Test fun `IR compilation of nested class within class matches corresponding data class toString`() {
        `compilation of nested class within class matches corresponding data class toString`(useIr = true)
    }

    private fun `compilation of nested class within class matches corresponding data class toString`(useIr: Boolean) =
        compareNestedClassInstances(useIr = useIr, nestedClassName = "Nested") { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }

    @Test fun `compilation of nested class within class matches corresponding data class hashCode`() {
        `compilation of nested class within class matches corresponding data class hashCode`(useIr = false)
    }

    @Test fun `IR compilation of nested class within class matches corresponding data class hashCode`() {
        `compilation of nested class within class matches corresponding data class hashCode`(useIr = true)
    }

    private fun `compilation of nested class within class matches corresponding data class hashCode`(useIr: Boolean) =
        compareNestedClassInstances(useIr = useIr, nestedClassName = "Nested") { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
        }

    @Test fun `compilation of nested class within interface matches corresponding data class toString`() {
        `compilation of nested class within interface matches corresponding data class toString`(useIr = false)
    }

    @Test fun `IR compilation of nested class within interface matches corresponding data class toString`() {
        `compilation of nested class within interface matches corresponding data class toString`(useIr = true)
    }

    private fun `compilation of nested class within interface matches corresponding data class toString`(
        useIr: Boolean
    ) = compareNestedClassInstances(
        useIr = useIr,
        nestedClassName = "Nested",
        outerClassName = "OuterInterface"
    ) { apiInstance, dataInstance ->
        assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
    }

    @Test fun `compilation of nested class within interface matches corresponding data class hashCode`() {
        `compilation of nested class within interface matches corresponding data class hashCode`(useIr = false)
    }

    @Test fun `IR compilation of nested class within interface matches corresponding data class hashCode`() {
        `compilation of nested class within interface matches corresponding data class hashCode`(useIr = true)
    }

    private fun `compilation of nested class within interface matches corresponding data class hashCode`(
        useIr: Boolean
    ) = compareNestedClassInstances(
        useIr = useIr,
        nestedClassName = "Nested",
        outerClassName = "OuterInterface"
    ) { apiInstance, dataInstance ->
        assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
    }

    private inline fun compareNestedClassInstances(
        useIr: Boolean,
        nestedClassName: String,
        outerClassName: String = "OuterClass",
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = compareWithDataClass(
        sourceFileName = outerClassName,
        className = "$outerClassName\$$nestedClassName",
        constructorArgs = listOf(String::class.java to "nested class value"),
        useIr = useIr,
        compare = compare
    )

    @Test fun `compilation of inner class fails`() {
        `compilation of inner class fails`(useIr = false)
    }

    @Test fun `IR compilation of inner class fails`() {
        `compilation of inner class fails`(useIr = true)
    }

    private fun `compilation of inner class fails`(useIr: Boolean) {
        testCompilation(
            "illegal/OuterClass",
            useIr = useIr,
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages).contains("@DataApi cannot be applied to inner classes")
        }
    }
    //endregion

    //region Simple class
    @Test fun `compiled Simple class instance has expected hashCode`() {
        `compiled Simple class instance has expected hashCode`(useIr = false)
    }

    @Test fun `IR-compiled Simple class instance has expected hashCode`() {
        `compiled Simple class instance has expected hashCode`(useIr = true)
    }

    private fun `compiled Simple class instance has expected hashCode`(useIr: Boolean) =
        compareSimpleClassInstances(useIr) { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
        }

    @Test fun `compiled Simple class instance has expected toString`() {
        `compiled Simple class instance has expected toString`(useIr = false)
    }

    @Test fun `IR-compiled Simple class instance has expected toString`() {
        `compiled Simple class instance has expected toString`(useIr = true)
    }

    private fun `compiled Simple class instance has expected toString`(useIr: Boolean) =
        compareSimpleClassInstances(useIr) { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }

    private inline fun compareSimpleClassInstances(
        useIr: Boolean,
        int: Int = 1,
        requiredString: String = "String",
        optionalString: String? = null,
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = compareWithDataClass(
        sourceFileName = "Simple",
        constructorArgs = listOf(
            Int::class.java to int,
            String::class.java to requiredString,
            String::class.java to optionalString
        ),
        useIr = useIr,
        compare = compare
    )
    //endregion

    //region Complex class
    @Test fun `compiled Complex class instance has expected hashCode`() {
        `compiled Complex class instance has expected hashCode`(useIr = false)
    }

    @Test fun `IR-compiled Complex class instance has expected hashCode`() {
        `compiled Complex class instance has expected hashCode`(useIr = true)
    }

    private fun `compiled Complex class instance has expected hashCode`(useIr: Boolean) =
        compareComplexClassInstances(useIr) { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
        }

    @Test fun `compiled Complex class instance has expected toString`() {
        `compiled Complex class instance has expected toString`(useIr = false)
    }

    @Test fun `IR-compiled Complex class instance has expected toString`() {
        `compiled Complex class instance has expected toString`(useIr = true)
    }

    private fun `compiled Complex class instance has expected toString`(useIr: Boolean) =
        compareComplexClassInstances(useIr) { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }

    private inline fun compareComplexClassInstances(
        useIr: Boolean,
        referenceType: String = "Text",
        nullableReferenceType: String? = null,
        int: Int = 2,
        nullableInt: Int? = null,
        long: Long = 12345L,
        float: Float = 67f,
        double: Double = 89.0,
        arrayReferenceType: Array<String> = arrayOf("one string", "another string"),
        nullableArrayReferenceType: Array<String>? = null,
        arrayPrimitiveType: IntArray = intArrayOf(3, 4, 5),
        nullableArrayPrimitiveType: IntArray? = null,
        genericCollectionType: List<BigDecimal> = listOf(6, 7, 8).map { BigDecimal(it) },
        nullableGenericCollectionType: List<BigDecimal>? = null,
        genericType: BigDecimal = BigDecimal(9),
        nullableGenericType: BigDecimal? = null,
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = compareWithDataClass(
        sourceFileName = "Complex",
        constructorArgs = listOf(
            String::class.java to referenceType,
            String::class.java to nullableReferenceType,
            Int::class.javaPrimitiveType!! to int,
            Int::class.javaObjectType to nullableInt,
            Long::class.javaPrimitiveType!! to long,
            Float::class.javaPrimitiveType!! to float,
            Double::class.javaPrimitiveType!! to double,
            Array<String>::class.java to arrayReferenceType,
            Array<String>::class.java to nullableArrayReferenceType,
            IntArray::class.java to arrayPrimitiveType,
            IntArray::class.java to nullableArrayPrimitiveType,
            List::class.java to genericCollectionType,
            List::class.java to nullableGenericCollectionType,
            Any::class.java to genericType,
            Any::class.java to nullableGenericType,
        ),
        useIr = useIr,
        compare = compare,
    )
    //endregion

    //region Helpers for all tests
    /**
     * Compiles and instantiates [sourceFileName] from both the `api` package and the `data` package, with the
     * expectation that they compile to a [DataApi] class and a data class, respectively. After instantiating each,
     * passes both to [compare] for comparison testing.
     */
    private inline fun compareWithDataClass(
        sourceFileName: String,
        className: String = sourceFileName,
        constructorArgs: List<Pair<Class<*>, Any?>>,
        otherFilesToCompile: List<String> = emptyList(),
        useIr: Boolean = false,
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = testCompilation(
        "api/$sourceFileName", "data/$sourceFileName", *otherFilesToCompile.toTypedArray(),
        useIr = useIr
    ) { result ->
        val apiClass = result.classLoader.loadClass("api.$className")
        val dataClass = result.classLoader.loadClass("data.$className")
        assertThat(apiClass).isNotEqualTo(dataClass)

        val constructorArgParameterTypes = constructorArgs.map { it.first }.toTypedArray()
        val apiConstructor = apiClass.getConstructor(*constructorArgParameterTypes)
        val dataConstructor = dataClass.getConstructor(*constructorArgParameterTypes)

        val constructorParameters = constructorArgs.map { it.second }.toTypedArray()
        val apiInstance = apiConstructor.newInstance(*constructorParameters)
        val dataInstance = dataConstructor.newInstance(*constructorParameters)

        assertThat(apiInstance.javaClass).isNotEqualTo(dataInstance.javaClass)
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
