package dev.drewhamilton.poko

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.prop
import assertk.assertions.startsWith
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import java.io.File
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@OptIn(ExperimentalCompilerApi::class)
@RunWith(TestParameterInjector::class)
class PokoCompilerPluginTest(
    @TestParameter private val k2: Boolean,
) {

    @JvmField
    @Rule var temporaryFolder: TemporaryFolder = TemporaryFolder()

    //region Primitives
    @Test fun `compilation of valid class succeeds`() {
        testCompilation("api/Primitives") { result ->
            val testClass = result.classLoader.loadClass("api.Primitives")
            val primaryTestInstance = testClass.declaredConstructors[0].newInstance(
                "a", 1f, 2.0, 3L, 4, 5.toShort(), 6.toByte(), true
            )
            val cloneOfPrimaryTestInstance = testClass.declaredConstructors[0].newInstance(
                "a", 1f, 2.0, 3L, 4, 5.toShort(), 6.toByte(), true
            )
            val otherTestInstance = testClass.declaredConstructors[0].newInstance(
                "b", 8f, 9.0, 11L, 4, 9.toShort(), 1.toByte(), false
            )

            assertThat(primaryTestInstance).all {
                prop(Any::toString).isEqualTo("Primitives(string=a, float=1.0, double=2.0, long=3, int=4, short=5, byte=6, boolean=true)")
                prop(Any::hashCode).isNotEqualTo(System.identityHashCode(primaryTestInstance))

                isEqualTo(primaryTestInstance)
                isEqualTo(cloneOfPrimaryTestInstance)
                isNotEqualTo(otherTestInstance)
            }
        }
    }
    //endregion

    //region ToStringGenerationDisabled
    @Test fun `compilation of valid class with disabled to string generation succeeds`() {
        testCompilation("api/ToStringGenerationDisabled") { result ->
            val testClass = result.classLoader.loadClass("api.ToStringGenerationDisabled")
            val testInstance = testClass.declaredConstructors[0].newInstance("hello world")
            // note @ at the end of assumed toString value, it indicates that default toString was used
            assertThat(testInstance.toString()).startsWith("api.ToStringGenerationDisabled@")
        }
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

    //region Nested
    @Test fun `compilation of inner class fails`() {
        testCompilation(
            "illegal/OuterClass",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages).contains("Poko cannot be applied to inner classes")
        }
    }
    //endregion

    //region Array content
    @Test fun `compilation reading array content of generic type with unsupported upper bound fails`() {
        testCompilation(
            "illegal/GenericArrayHolder",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages)
                .contains("@ArrayContentBased on property of type <G of illegal.GenericArrayHolder> not supported")
        }
    }

    @Test fun `compilation reading array content of non-arrays fails`() {
        testCompilation(
            "illegal/NotArrayHolder",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            assertThat(result.messages)
                .contains("@ArrayContentBased on property of type <kotlin.String> not supported")
            assertThat(result.messages)
                .contains("@ArrayContentBased on property of type <kotlin.Int> not supported")
            assertThat(result.messages)
                .contains("@ArrayContentBased on property of type <kotlin.Float> not supported")
        }
    }
    //endregion

    //region Unknown annotation name
    @Test fun `unknown annotation name produces expected error message`() {
        testCompilation(
            "api/Primitives",
            pokoAnnotationName = "nonexistent/ClassName",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
        ) {
            assertThat(it.messages).contains("e: Could not find class <nonexistent/ClassName>\n")
        }
    }
    //endregion

    private inline fun testCompilation(
        vararg sourceFileNames: String,
        pokoAnnotationName: String = "dev/drewhamilton/poko/Poko",
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

    private fun prepareCompilation(
        vararg sourceFiles: SourceFile,
        pokoAnnotationName: String,
    ) = KotlinCompilation().apply {
        workingDir = temporaryFolder.root
        compilerPluginRegistrars = listOf(PokoCompilerPluginRegistrar())
        inheritClassPath = true
        sources = sourceFiles.asList()
        verbose = false
        jvmTarget = JvmTarget.JVM_1_8.description
        useIR = true
        if (k2) {
            languageVersion = "2.0"
        }

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

}
