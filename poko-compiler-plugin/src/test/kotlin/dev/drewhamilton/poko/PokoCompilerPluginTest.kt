package dev.drewhamilton.poko

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import java.io.File
import java.math.BigDecimal
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCompilerApi::class)
class PokoCompilerPluginTest {

    @JvmField
    @Rule var temporaryFolder: TemporaryFolder = TemporaryFolder()

    //region Primitives
    @Test fun `compilation of valid class succeeds`() {
        testCompilation("api/Primitives")
    }
    //endregion

    //region data class
    @Test fun `compilation of data class fails`() {
        testCompilation(
            "illegal/Data",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages).contains("Poko does not support data classes")
        }
    }
    //endregion

    //region value class
    @Test fun `compilation of value class fails`() {
        testCompilation(
            "illegal/Value",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages).contains("Poko does not support value classes")
        }
    }
    //endregion

    //region No primary constructor
    @Test fun `compilation without primary constructor fails`() {
        testCompilation(
            "illegal/NoPrimaryConstructor",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages).contains("Poko classes must have a primary constructor")
        }
    }
    //endregion

    //region No constructor properties
    @Test fun `compilation without constructor properties fails`() {
        testCompilation(
            "illegal/NoConstructorProperties",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages)
                .contains("Poko classes must have at least one property in the primary constructor")
        }
    }
    //endregion

    //region Explicit function declarations
    @Test fun `two equivalent compiled ExplicitDeclarations instances are equals`() =
        compareTwoExplicitDeclarationsApiInstances { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled ExplicitDeclarations instances are not equals`() =
        compareTwoExplicitDeclarationsApiInstances(string2 = "string 11") { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoExplicitDeclarationsApiInstances(
        string1: String = "string 1",
        string2: String = "string 2",
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = compareTwoInstances(
        sourceFileName = "api/ExplicitDeclarations",
        firstInstanceConstructorArgs = listOf(String::class.java to string1),
        secondInstanceConstructorArgs = listOf(String::class.java to string2),
        compare = compare
    )

    @Test fun `compilation with explicit function declarations respects explicit hashCode`() {
        val testString = "test thing"
        compareExplicitDeclarationsInstances(string = testString) { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(testString.length)
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
        }
    }

    @Test fun `compilation with explicit function declarations respects explicit toString`() {
        val testString = "test string"
        compareExplicitDeclarationsInstances(string = testString) { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(testString)
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }
    }

    private fun compareExplicitDeclarationsInstances(
        string: String = "test string",
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = compareApiWithDataClass(
        sourceFileName = "ExplicitDeclarations",
        constructorArgs = listOf(String::class.java to string),
        compare = compare
    )
    //endregion

    //region Superclass function declarations
    @Test fun `two equivalent compiled Subclass instances are equals`() =
        compareTwoSubclassApiInstances { firstInstance, secondInstance ->
            // Super class equals implementation returns `other == true`; this confirms that is overridden:
            assertThat(firstInstance).isNotEqualTo(true)
            assertThat(secondInstance).isNotEqualTo(true)

            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled Subclass instances are not equals`() =
        compareTwoSubclassApiInstances(number2 = 888) { firstInstance, secondInstance ->
            // Super class equals implementation returns `other == true`; this confirms that is overridden:
            assertThat(firstInstance).isNotEqualTo(true)
            assertThat(secondInstance).isNotEqualTo(true)

            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoSubclassApiInstances(
        number1: Number = 999,
        number2: Number = number1,
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = compareTwoInstances(
        sourceFileName = "api/Sub",
        firstInstanceConstructorArgs = listOf(Number::class.java to number1),
        secondInstanceConstructorArgs = listOf(Number::class.java to number2),
        otherFilesToCompile = listOf("Super"),
        compare = compare
    )

    @Test fun `superclass hashCode is overridden`() =
        compareSubclassInstances { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
            assertThat(apiInstance.hashCode()).isNotEqualTo(50934)
        }

    @Test fun `superclass toString is overridden`() =
        compareSubclassInstances { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
            assertThat(apiInstance.toString()).isNotEqualTo("superclass")
        }

    private fun compareSubclassInstances(
        number: Number = 123.4,
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = compareApiWithDataClass(
        sourceFileName = "Sub",
        constructorArgs = listOf(Number::class.java to number),
        otherFilesToCompile = listOf("Super"),
        compare = compare
    )
    //endregion

    //region Nested
    @Test fun `two equivalent compiled Nested instances are equals`() =
        compareTwoNestedApiInstances { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled Nested instances are not equals`() =
        compareTwoNestedApiInstances(value2 = "string 2") { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoNestedApiInstances(
        value1: String = "string 1",
        value2: String = value1,
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = compareTwoInstances(
        sourceFileName = "api/OuterClass",
        className = "api.OuterClass\$Nested",
        firstInstanceConstructorArgs = listOf(String::class.java to value1),
        secondInstanceConstructorArgs = listOf(String::class.java to value2),
        compare = compare
    )

    @Test fun `compilation of nested class within class matches corresponding data class toString`() =
        compareNestedClassApiAndDataInstances(nestedClassName = "Nested") { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }

    @Test fun `compilation of nested class within class matches corresponding data class hashCode`() =
        compareNestedClassApiAndDataInstances(nestedClassName = "Nested") { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
        }

    @Test fun `compilation of nested class within interface matches corresponding data class toString`() =
        compareNestedClassApiAndDataInstances(
            nestedClassName = "Nested",
            outerClassName = "OuterInterface"
        ) { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }

    @Test fun `compilation of nested class within interface matches corresponding data class hashCode`() =
        compareNestedClassApiAndDataInstances(
            nestedClassName = "Nested",
            outerClassName = "OuterInterface"
        ) { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
        }

    private inline fun compareNestedClassApiAndDataInstances(
        nestedClassName: String,
        outerClassName: String = "OuterClass",
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = compareApiWithDataClass(
        sourceFileName = outerClassName,
        className = "$outerClassName\$$nestedClassName",
        constructorArgs = listOf(String::class.java to "nested class value"),
        compare = compare
    )

    @Test fun `compilation of inner class fails`() {
        testCompilation(
            "illegal/OuterClass",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages).contains("Poko cannot be applied to inner classes")
        }
    }
    //endregion

    //region Simple class
    @Test fun `two equivalent compiled Simple instances are equals`() =
        compareTwoSimpleApiInstances { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled Simple instances are not equals`() =
        compareTwoSimpleApiInstances(optionalString2 = "non-null") { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoSimpleApiInstances(
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
        compare = compare
    )

    @Test fun `compiled Simple class instance has expected hashCode`() =
        compareSimpleClassInstances { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
        }

    @Test fun `compiled Simple class instance has expected toString`() =
        compareSimpleClassInstances { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }

    private inline fun compareSimpleClassInstances(
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
        compare = compare
    )
    //endregion

    //region SimpleWithExtraParam
    @Test fun `non-property parameter is ignored for equals`() {
        compareTwoSimpleWithExtraParamApiInstances(
            callback1 = { true },
            callback2 = { false },
        ) { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }
    }

    @Test fun `non-property parameter is ignored for hashCode`() {
        compareTwoSimpleWithExtraParamApiInstances(
            callback1 = { true },
            callback2 = { false },
        ) { firstInstance, secondInstance ->
            assertThat(firstInstance.hashCode()).isEqualTo(secondInstance.hashCode())
        }
    }

    @Test fun `non-property parameter is ignored for toString`() {
        compareTwoSimpleWithExtraParamApiInstances(
            callback1 = { true },
            callback2 = { false },
        ) { firstInstance, secondInstance ->
            assertThat(firstInstance.toString()).isEqualTo(secondInstance.toString())
        }
    }

    private fun compareTwoSimpleWithExtraParamApiInstances(
        callback1: (Unit) -> Boolean,
        callback2: (Unit) -> Boolean,
        int1: Int = 1,
        requiredString1: String = "String",
        optionalString1: String? = null,
        int2: Int = int1,
        requiredString2: String = requiredString1,
        optionalString2: String? = optionalString1,
        compare: (firstInstance: Any, secondInstance: Any) -> Unit,
    ) = compareTwoInstances(
        sourceFileName = "api/SimpleWithExtraParam",
        firstInstanceConstructorArgs = listOf(
            Int::class.java to int1,
            String::class.java to requiredString1,
            String::class.java to optionalString1,
            Function1::class.java to callback1,
        ),
        secondInstanceConstructorArgs = listOf(
            Int::class.java to int2,
            String::class.java to requiredString2,
            String::class.java to optionalString2,
            Function1::class.java to callback2,
        ),
        compare = compare
    )
    //endregion

    //region Complex class
    @Test fun `two equivalent compiled Complex instances are equals`() =
        compareTwoComplexApiInstances { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }

    @Test fun `two inequivalent compiled Complex instances are not equals`() =
        compareTwoComplexApiInstances(nullableReferenceType2 = "non-null") { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }

    private fun compareTwoComplexApiInstances(
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
        compare = compare
    )

    @Test fun `compiled Complex class instance has expected hashCode`() =
        compareComplexClassInstances { apiInstance, dataInstance ->
            assertThat(apiInstance.hashCode()).isEqualTo(dataInstance.hashCode())
        }

    @Test fun `compiled Complex class instance has expected toString`() =
        compareComplexClassInstances { apiInstance, dataInstance ->
            assertThat(apiInstance.toString()).isEqualTo(dataInstance.toString())
        }

    private inline fun compareComplexClassInstances(
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
        compare = compare,
    )
    //endregion

    //region Array content
    @Test fun `two equivalent compiled ArrayHolder instances are equals`() {
        compareTwoArrayHolderApiInstances { firstInstance, secondInstance ->
            assertThat(firstInstance).isEqualTo(secondInstance)
            assertThat(secondInstance).isEqualTo(firstInstance)
        }
    }

    @Test fun `two equivalent compiled ArrayHolder instances have same hashCode`() {
        compareTwoArrayHolderApiInstances { firstInstance, secondInstance ->
            assertThat(firstInstance.hashCode()).isEqualTo(secondInstance.hashCode())
        }
    }

    @Test fun `two equivalent compiled ArrayHolder instances have same toString`() {
        compareTwoArrayHolderApiInstances { firstInstance, secondInstance ->
            assertThat(firstInstance.toString()).isEqualTo(secondInstance.toString())
        }
    }

    @Test fun `two inequivalent compiled ArrayHolder instances are not equals`() {
        compareTwoArrayHolderApiInstances(
            stringArray2 = arrayOf("just one string"),
        ) { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }
        compareTwoArrayHolderApiInstances(
            charArray2 = charArrayOf('x', 'y', 'z'),
        ) { firstInstance, secondInstance ->
            assertThat(firstInstance).isNotEqualTo(secondInstance)
            assertThat(secondInstance).isNotEqualTo(firstInstance)
        }
    }

    private fun compareTwoArrayHolderApiInstances(
        stringArray1: Array<String> = arrayOf("one string", "another string"),
        nullableStringArray1: Array<String>? = null,
        booleanArray1: BooleanArray = booleanArrayOf(true, false),
        nullableBooleanArray1: BooleanArray? = null,
        byteArray1: ByteArray = byteArrayOf(1.toByte(), 2.toByte()),
        nullableByteArray1: ByteArray? = null,
        charArray1: CharArray = charArrayOf('a', 'b', 'c', 'c'),
        nullableCharArray1: CharArray? = null,
        shortArray1: ShortArray = shortArrayOf(99.toShort(), 88.toShort()),
        nullableShortArray1: ShortArray? = null,
        intArray1: IntArray = intArrayOf(3, 4, 5),
        nullableIntArray1: IntArray? = null,
        longArray1: LongArray = longArrayOf(98765L, 43210L),
        nullableLongArray1: LongArray? = null,
        floatArray1: FloatArray = floatArrayOf(3.14f, 1519f),
        nullableFloatArray1: FloatArray? = null,
        doubleArray1: DoubleArray = doubleArrayOf(2.22222, Double.NaN),
        nullableDoubleArray1: DoubleArray? = null,
        stringArray2: Array<String> = stringArray1,
        nullableStringArray2: Array<String>? = nullableStringArray1,
        booleanArray2: BooleanArray = booleanArray1,
        nullableBooleanArray2: BooleanArray? = nullableBooleanArray1,
        byteArray2: ByteArray = byteArray1,
        nullableByteArray2: ByteArray? = nullableByteArray1,
        charArray2: CharArray = charArray1,
        nullableCharArray2: CharArray? = nullableCharArray1,
        shortArray2: ShortArray = shortArray1,
        nullableShortArray2: ShortArray? = nullableShortArray1,
        intArray2: IntArray = intArray1,
        nullableIntArray2: IntArray? = nullableIntArray1,
        longArray2: LongArray = longArray1,
        nullableLongArray2: LongArray? = nullableLongArray1,
        floatArray2: FloatArray = floatArray1,
        nullableFloatArray2: FloatArray? = nullableFloatArray1,
        doubleArray2: DoubleArray = doubleArray1,
        nullableDoubleArray2: DoubleArray? = nullableDoubleArray1,
        compare: (firstInstance: Any, secondInstance: Any) -> Unit,
    ) = compareTwoInstances(
        sourceFileName = "api/ArrayHolder",
        firstInstanceConstructorArgs = listOf(
            Array<String>::class.java to stringArray1,
            Array<String>::class.java to nullableStringArray1,
            BooleanArray::class.java to booleanArray1,
            BooleanArray::class.java to nullableBooleanArray1,
            ByteArray::class.java to byteArray1,
            ByteArray::class.java to nullableByteArray1,
            CharArray::class.java to charArray1,
            CharArray::class.java to nullableCharArray1,
            ShortArray::class.java to shortArray1,
            ShortArray::class.java to nullableShortArray1,
            IntArray::class.java to intArray1,
            IntArray::class.java to nullableIntArray1,
            LongArray::class.java to longArray1,
            LongArray::class.java to nullableLongArray1,
            FloatArray::class.java to floatArray1,
            FloatArray::class.java to nullableFloatArray1,
            DoubleArray::class.java to doubleArray1,
            DoubleArray::class.java to nullableDoubleArray1,
        ),
        secondInstanceConstructorArgs = listOf(
            Array<String>::class.java to stringArray2,
            Array<String>::class.java to nullableStringArray2,
            BooleanArray::class.java to booleanArray2,
            BooleanArray::class.java to nullableBooleanArray2,
            ByteArray::class.java to byteArray2,
            ByteArray::class.java to nullableByteArray2,
            CharArray::class.java to charArray2,
            CharArray::class.java to nullableCharArray2,
            ShortArray::class.java to shortArray2,
            ShortArray::class.java to nullableShortArray2,
            IntArray::class.java to intArray2,
            IntArray::class.java to nullableIntArray2,
            LongArray::class.java to longArray2,
            LongArray::class.java to nullableLongArray2,
            FloatArray::class.java to floatArray2,
            FloatArray::class.java to nullableFloatArray2,
            DoubleArray::class.java to doubleArray2,
            DoubleArray::class.java to nullableDoubleArray2,
        ),
        compare = compare,
    )

    @Test fun `compilation reading array content of type Any fails`() {
        testCompilation(
            "illegal/AnyArrayHolder",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages)
                .contains("@ReadArrayContent on property of type <kotlin.Any?> not supported")
        }
    }

    @Test fun `compilation reading array content of generic type fails`() {
        testCompilation(
            "illegal/GenericArrayHolder",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages)
                .contains("@ReadArrayContent on property of type <G of illegal.GenericArrayHolder> not supported")
        }
    }

    @Test fun `compilation reading array content of nested primitive array fails`() {
        testCompilation(
            "illegal/NestedPrimitiveArrayHolder",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages)
                .contains("@ReadArrayContent on nested array property not supported")
        }
    }

    @Test fun `compilation reading array content of nested typed array fails`() {
        testCompilation(
            "illegal/NestedTypedArrayHolder",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages)
                .contains("@ReadArrayContent on nested array property not supported")
        }
    }

    @Test fun `compilation reading array content of non-arrays fails`() {
        testCompilation(
            "illegal/NotArrayHolder",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages)
                .contains("@ReadArrayContent on property of type <kotlin.String> not supported")
            assertThat(result.messages)
                .contains("@ReadArrayContent on property of type <kotlin.Int> not supported")
            assertThat(result.messages)
                .contains("@ReadArrayContent on property of type <kotlin.Float> not supported")
        }
    }
    //endregion

    //region Unknown annotation name
    @Test fun `unknown annotation name produces expected error message`() {
        testCompilation(
            "api/Simple",
            pokoAnnotationName = "nonexistent/ClassName",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
        ) {
            assertThat(it.messages).isEqualTo("e: Could not find class <nonexistent/ClassName>\n")
        }
    }
    //endregion

    //region Helpers for all tests
    private inline fun compareTwoInstances(
        sourceFileName: String,
        className: String = sourceFileName.replace('/', '.'),
        firstInstanceConstructorArgs: List<Pair<Class<*>, Any?>>,
        secondInstanceConstructorArgs: List<Pair<Class<*>, Any?>> = firstInstanceConstructorArgs,
        otherFilesToCompile: List<String> = emptyList(),
        compare: (firstInstance: Any, secondInstance: Any) -> Unit
    ) = testCompilation(sourceFileName, *otherFilesToCompile.toTypedArray()) { result ->
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
     * expectation that they compile to a [Poko] class and a data class, respectively. After instantiating each,
     * passes both to [compare] for comparison testing.
     */
    private inline fun compareApiWithDataClass(
        sourceFileName: String,
        className: String = sourceFileName,
        constructorArgs: List<Pair<Class<*>, Any?>>,
        otherFilesToCompile: List<String> = emptyList(),
        compare: (apiInstance: Any, dataInstance: Any) -> Unit
    ) = testCompilation("api/$sourceFileName", "data/$sourceFileName", *otherFilesToCompile.toTypedArray()) { result ->
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
        pokoAnnotationName: String = Poko::class.java.name,
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (KotlinCompilation.Result) -> Unit = {}
    ) = testCompilation(
        *sourceFileNames.map { SourceFile.fromPath("src/test/resources/$it.kt") }.toTypedArray(),
        pokoAnnotationName = pokoAnnotationName,
        expectedExitCode = expectedExitCode,
        additionalTesting = additionalTesting
    )

    private inline fun testCompilation(
        vararg sourceFiles: SourceFile,
        pokoAnnotationName: String,
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (KotlinCompilation.Result) -> Unit = {}
    ) {
        val result =
            prepareCompilation(*sourceFiles, pokoAnnotationName = pokoAnnotationName).compile()
        if (
            expectedExitCode == KotlinCompilation.ExitCode.OK &&
            result.exitCode != KotlinCompilation.ExitCode.OK
        ) {
            val failureMessage = StringBuilder().apply {
                append("expected: OK\nbut was : ")
                append(result.exitCode)
                append("\n")

                result.messages.split("\n").forEach { message ->
                    if (message.isNotEmpty()) {
                        append("- ")
                        append(message)
                        append("\n")
                    }
                }
            }.toString()
            fail(failureMessage)
        } else {
            assertThat(result.exitCode).isEqualTo(expectedExitCode)
        }
        additionalTesting(result)
    }

    @OptIn(FirIncompatiblePluginAPI::class)
    private fun prepareCompilation(
        vararg sourceFiles: SourceFile,
        pokoAnnotationName: String,
    ) = KotlinCompilation().apply {
        workingDir = temporaryFolder.root
        compilerPluginRegistrars = listOf(PokoCompilerPluginRegistrar())
        inheritClassPath = true
        sources = sourceFiles.asList()
        verbose = false
        jvmTarget = compilerJvmTarget.description
        useIR = true

        val commandLineProcessor = PokoCommandLineProcessor()
        commandLineProcessors = listOf(commandLineProcessor)
        pluginOptions = listOf(
            commandLineProcessor.option(CompilerOptions.ENABLED, true),
            commandLineProcessor.option(CompilerOptions.POKO_ANNOTATION, pokoAnnotationName)
        )
    }

    private fun CommandLineProcessor.option(key: CompilerConfigurationKey<*>, value: Any?) = PluginOption(
        pluginId,
        key.toString(),
        value.toString()
    )

    private fun SourceFile.Companion.fromPath(path: String): SourceFile = fromPath(File(path))
    //endregion

    companion object {

        private val compilerJvmTarget: JvmTarget by lazy {
            val resolvedJvmDescription = getJavaRuntimeVersion()
            val resolvedJvmTarget = JvmTarget.fromString(resolvedJvmDescription)
            val default = JvmTarget.JVM_1_8

            val message = if (resolvedJvmTarget == null) {
                "${default.description} because test runtime JVM version <$resolvedJvmDescription> was not valid"
            } else {
                "${resolvedJvmTarget.description} determined from test runtime JVM"
            }
            println("${PokoCompilerPluginTest::class.java.simpleName}: Using jvmTarget $message")

            resolvedJvmTarget ?: default
        }

        private fun getJavaRuntimeVersion(): String {
            val runtimeVersionArray = System.getProperty("java.runtime.version").split(".", "_", "-b")
            val prefix = runtimeVersionArray[0]
            return if (prefix == "1")
                "1.${runtimeVersionArray[1]}"
            else
                prefix
        }
    }
}
