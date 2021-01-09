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
    @Test fun `two equivalent compiled ExplicitDeclarations instances are equals`() {
        `two equivalent compiled ExplicitDeclarations instances are equals`(useIr = false)
    }

    @Test fun `two equivalent IR-compiled ExplicitDeclarations instances are equals`() {
        `two equivalent compiled ExplicitDeclarations instances are equals`(useIr = true)
    }

    private fun `two equivalent compiled ExplicitDeclarations instances are equals`(useIr: Boolean) =
        compareTwoExplicitDeclarationsApiInstances(useIr) { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled ExplicitDeclarations instances are not equals`() {
        `two inequivalent compiled ExplicitDeclarations instances are not equals`(useIr = false)
    }

    @Test fun `two inequivalent IR-compiled ExplicitDeclarations instances are not equals`() {
        `two inequivalent compiled ExplicitDeclarations instances are not equals`(useIr = true)
    }

    private fun `two inequivalent compiled ExplicitDeclarations instances are not equals`(useIr: Boolean) =
        compareTwoExplicitDeclarationsApiInstances(useIr, string2 = "string 11") { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoExplicitDeclarationsApiInstances(
        useIr: Boolean,
        string1: String = "string 1",
        string2: String = "string 2",
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = compareTwoInstances(
        sourceFileName = "api/ExplicitDeclarations",
        firstInstanceConstructorArgs = listOf(String::class.java to string1),
        secondInstanceConstructorArgs = listOf(String::class.java to string2),
        useIr = useIr,
        compare = compare
    )

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
    ) = compareApiWithDataClass(
        sourceFileName = "ExplicitDeclarations",
        constructorArgs = listOf(String::class.java to string),
        useIr = useIr,
        compare = compare
    )
    //endregion

    //region Superclass function declarations
    @Test fun `two equivalent compiled Subclass instances are equals`() {
        `two equivalent compiled Subclass instances are equals`(useIr = false)
    }

    @Test fun `two equivalent IR-compiled Subclass instances are equals`() {
        `two equivalent compiled Subclass instances are equals`(useIr = true)
    }

    private fun `two equivalent compiled Subclass instances are equals`(useIr: Boolean) =
        compareTwoSubclassApiInstances(useIr) { firstInstance, secondInstance ->
            // Super class equals implementation returns `other == true`; this confirms that is overridden:
            assertThat(firstInstance).isNotEqualTo(true)
            assertThat(secondInstance).isNotEqualTo(true)

            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled Subclass instances are not equals`() {
        `two inequivalent compiled Subclass instances are not equals`(useIr = false)
    }

    @Test fun `two inequivalent IR-compiled Subclass instances are not equals`() {
        `two inequivalent compiled Subclass instances are not equals`(useIr = true)
    }

    private fun `two inequivalent compiled Subclass instances are not equals`(useIr: Boolean) =
        compareTwoSubclassApiInstances(useIr, number2 = 888) { firstInstance, secondInstance ->
            // Super class equals implementation returns `other == true`; this confirms that is overridden:
            assertThat(firstInstance).isNotEqualTo(true)
            assertThat(secondInstance).isNotEqualTo(true)

            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoSubclassApiInstances(
        useIr: Boolean,
        number1: Number = 999,
        number2: Number = number1,
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = compareTwoInstances(
        sourceFileName = "api/Sub",
        firstInstanceConstructorArgs = listOf(Number::class.java to number1),
        secondInstanceConstructorArgs = listOf(Number::class.java to number2),
        otherFilesToCompile = listOf("Super"),
        useIr = useIr,
        compare = compare
    )

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
    ) = compareApiWithDataClass(
        sourceFileName = "Sub",
        constructorArgs = listOf(Number::class.java to number),
        otherFilesToCompile = listOf("Super"),
        useIr = useIr,
        compare = compare
    )
    //endregion

    //region Nested
    @Test fun `two equivalent compiled Nested instances are equals`() {
        `two equivalent compiled Nested instances are equals`(useIr = false)
    }

    @Test fun `two equivalent IR-compiled Nested instances are equals`() {
        `two equivalent compiled Nested instances are equals`(useIr = true)
    }

    private fun `two equivalent compiled Nested instances are equals`(useIr: Boolean) =
        compareTwoNestedApiInstances(useIr) { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled Nested instances are not equals`() {
        `two inequivalent compiled Nested instances are not equals`(useIr = false)
    }

    @Test fun `two inequivalent IR-compiled Nested instances are not equals`() {
        `two inequivalent compiled Nested instances are not equals`(useIr = true)
    }

    private fun `two inequivalent compiled Nested instances are not equals`(useIr: Boolean) =
        compareTwoNestedApiInstances(useIr, value2 = "string 2") { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoNestedApiInstances(
        useIr: Boolean,
        value1: String = "string 1",
        value2: String = value1,
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = compareTwoInstances(
        sourceFileName = "api/OuterClass",
        className = "api.OuterClass\$Nested",
        firstInstanceConstructorArgs = listOf(String::class.java to value1),
        secondInstanceConstructorArgs = listOf(String::class.java to value2),
        useIr = useIr,
        compare = compare
    )

    @Test fun `compilation of nested class within class matches corresponding data class toString`() {
        `compilation of nested class within class matches corresponding data class toString`(useIr = false)
    }

    @Test fun `IR compilation of nested class within class matches corresponding data class toString`() {
        `compilation of nested class within class matches corresponding data class toString`(useIr = true)
    }

    private fun `compilation of nested class within class matches corresponding data class toString`(useIr: Boolean) =
        compareNestedClassApiAndDataInstances(useIr = useIr, nestedClassName = "Nested") { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }

    @Test fun `compilation of nested class within class matches corresponding data class hashCode`() {
        `compilation of nested class within class matches corresponding data class hashCode`(useIr = false)
    }

    @Test fun `IR compilation of nested class within class matches corresponding data class hashCode`() {
        `compilation of nested class within class matches corresponding data class hashCode`(useIr = true)
    }

    private fun `compilation of nested class within class matches corresponding data class hashCode`(useIr: Boolean) =
        compareNestedClassApiAndDataInstances(useIr = useIr, nestedClassName = "Nested") { apiInstance, dataInstance ->
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
    ) = compareNestedClassApiAndDataInstances(
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
    ) = compareNestedClassApiAndDataInstances(
        useIr = useIr,
        nestedClassName = "Nested",
        outerClassName = "OuterInterface"
    ) { apiInstance, dataInstance ->
        assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
    }

    private inline fun compareNestedClassApiAndDataInstances(
        useIr: Boolean,
        nestedClassName: String,
        outerClassName: String = "OuterClass",
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = compareApiWithDataClass(
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
    @Test fun `two equivalent compiled Simple instances are equals`() {
        `two equivalent compiled Simple instances are equals`(useIr = false)
    }

    @Test fun `two equivalent IR-compiled Simple instances are equals`() {
        `two equivalent compiled Simple instances are equals`(useIr = true)
    }

    private fun `two equivalent compiled Simple instances are equals`(useIr: Boolean) =
        compareTwoSimpleApiInstances(useIr) { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled Simple instances are not equals`() {
        `two inequivalent compiled Simple instances are not equals`(useIr = false)
    }

    @Test fun `two inequivalent IR-compiled Simple instances are not equals`() {
        `two inequivalent compiled Simple instances are not equals`(useIr = true)
    }

    private fun `two inequivalent compiled Simple instances are not equals`(useIr: Boolean) =
        compareTwoSimpleApiInstances(useIr, optionalString2 = "non-null") { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoSimpleApiInstances(
        useIr: Boolean,
        int1: Int = 1,
        requiredString1: String = "String",
        optionalString1: String? = null,
        int2: Int = int1,
        requiredString2: String = requiredString1,
        optionalString2: String? = optionalString1,
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = compareTwoInstances(
        sourceFileName = "api/Simple",
        firstInstanceConstructorArgs = listOf(
            Int::class.java to int1,
            String::class.java to requiredString1,
            String::class.java to optionalString1
        ),
        secondInstanceConstructorArgs = listOf(
            Int::class.java to int2,
            String::class.java to requiredString2,
            String::class.java to optionalString2
        ),
        useIr = useIr,
        compare = compare
    )

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
    ) = compareApiWithDataClass(
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
    @Test fun `two equivalent compiled Complex instances are equals`() {
        `two equivalent compiled Complex instances are equals`(useIr = false)
    }

    @Test fun `two equivalent IR-compiled Complex instances are equals`() {
        `two equivalent compiled Complex instances are equals`(useIr = true)
    }

    private fun `two equivalent compiled Complex instances are equals`(useIr: Boolean) =
        compareTwoComplexApiInstances(useIr) { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled Complex instances are not equals`() {
        `two inequivalent compiled Complex instances are not equals`(useIr = false)
    }

    @Test fun `two inequivalent IR-compiled Complex instances are not equals`() {
        `two inequivalent compiled Complex instances are not equals`(useIr = true)
    }

    private fun `two inequivalent compiled Complex instances are not equals`(useIr: Boolean) =
        compareTwoComplexApiInstances(useIr, nullableReferenceType2 = "non-null") { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoComplexApiInstances(
        useIr: Boolean,
        referenceType1: String = "Text",
        nullableReferenceType1: String? = null,
        int1: Int = 2,
        nullableInt1: Int? = null,
        long1: Long = 12345L,
        float1: Float = 67f,
        double1: Double = 89.0,
        arrayReferenceType1: Array<String> = arrayOf("one string", "another string"),
        nullableArrayReferenceType1: Array<String>? = null,
        arrayPrimitiveType1: IntArray = intArrayOf(3, 4, 5),
        nullableArrayPrimitiveType1: IntArray? = null,
        genericCollectionType1: List<BigDecimal> = listOf(6, 7, 8).map { BigDecimal(it) },
        nullableGenericCollectionType1: List<BigDecimal>? = null,
        genericType1: BigDecimal = BigDecimal(9),
        nullableGenericType1: BigDecimal? = null,
        referenceType2: String = referenceType1,
        nullableReferenceType2: String? = nullableReferenceType1,
        int2: Int = int1,
        nullableInt2: Int? = nullableInt1,
        long2: Long = long1,
        float2: Float = float1,
        double2: Double = double1,
        arrayReferenceType2: Array<String> = arrayReferenceType1,
        nullableArrayReferenceType2: Array<String>? = nullableArrayReferenceType1,
        arrayPrimitiveType2: IntArray = arrayPrimitiveType1,
        nullableArrayPrimitiveType2: IntArray? = nullableArrayPrimitiveType1,
        genericCollectionType2: List<BigDecimal> = genericCollectionType1,
        nullableGenericCollectionType2: List<BigDecimal>? = nullableGenericCollectionType1,
        genericType2: BigDecimal = genericType1,
        nullableGenericType2: BigDecimal? = nullableGenericType1,
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = compareTwoInstances(
        sourceFileName = "api/Complex",
        firstInstanceConstructorArgs = listOf(
            String::class.java to referenceType1,
            String::class.java to nullableReferenceType1,
            Int::class.javaPrimitiveType!! to int1,
            Int::class.javaObjectType to nullableInt1,
            Long::class.javaPrimitiveType!! to long1,
            Float::class.javaPrimitiveType!! to float1,
            Double::class.javaPrimitiveType!! to double1,
            Array<String>::class.java to arrayReferenceType1,
            Array<String>::class.java to nullableArrayReferenceType1,
            IntArray::class.java to arrayPrimitiveType1,
            IntArray::class.java to nullableArrayPrimitiveType1,
            List::class.java to genericCollectionType1,
            List::class.java to nullableGenericCollectionType1,
            Any::class.java to genericType1,
            Any::class.java to nullableGenericType1,
        ),
        secondInstanceConstructorArgs = listOf(
            String::class.java to referenceType2,
            String::class.java to nullableReferenceType2,
            Int::class.javaPrimitiveType!! to int2,
            Int::class.javaObjectType to nullableInt2,
            Long::class.javaPrimitiveType!! to long2,
            Float::class.javaPrimitiveType!! to float2,
            Double::class.javaPrimitiveType!! to double2,
            Array<String>::class.java to arrayReferenceType2,
            Array<String>::class.java to nullableArrayReferenceType2,
            IntArray::class.java to arrayPrimitiveType2,
            IntArray::class.java to nullableArrayPrimitiveType2,
            List::class.java to genericCollectionType2,
            List::class.java to nullableGenericCollectionType2,
            Any::class.java to genericType2,
            Any::class.java to nullableGenericType2,
        ),
        useIr = useIr,
        compare = compare
    )

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
    ) = compareApiWithDataClass(
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
    private inline fun compareTwoInstances(
        sourceFileName: String,
        className: String = sourceFileName.replace('/', '.'),
        firstInstanceConstructorArgs: List<Pair<Class<*>, Any?>>,
        secondInstanceConstructorArgs: List<Pair<Class<*>, Any?>> = firstInstanceConstructorArgs,
        otherFilesToCompile: List<String> = emptyList(),
        useIr: Boolean = false,
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = testCompilation(sourceFileName, *otherFilesToCompile.toTypedArray(), useIr = useIr) { result ->
        val constructorArgParameterTypeList = firstInstanceConstructorArgs.map { it.first }
        assertThat(constructorArgParameterTypeList).isEqualTo(secondInstanceConstructorArgs.map { it.first })

        val clazz = result.classLoader.loadClass(className)

        val constructor = clazz.getConstructor(*constructorArgParameterTypeList.toTypedArray())

        val firstConstructorParameters = firstInstanceConstructorArgs.map { it.second }.toTypedArray()
        val firstInstance = constructor.newInstance(*firstConstructorParameters)

        val secondConstructorParameters = secondInstanceConstructorArgs.map { it.second }.toTypedArray()
        val secondInstance = constructor.newInstance(*secondConstructorParameters)

        compare(firstInstance, secondInstance)
    }

    /**
     * Compiles and instantiates [sourceFileName] from both the `api` package and the `data` package, with the
     * expectation that they compile to a [DataApi] class and a data class, respectively. After instantiating each,
     * passes both to [compare] for comparison testing.
     */
    private inline fun compareApiWithDataClass(
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
