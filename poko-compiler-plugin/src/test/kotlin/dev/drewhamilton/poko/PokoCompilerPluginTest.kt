package dev.drewhamilton.poko

import assertk.all
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.single
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import dev.drewhamilton.poko.test.parameters
import dev.drewhamilton.poko.test.returnType
import dev.drewhamilton.poko.test.type
import java.io.File
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.runtime.components.tryLoadClass
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
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

    @Test fun `compilation of valid class succeeds`() {
        testCompilation("api/Primitives")
    }

    @Test fun `compilation of interface fails`() {
        testCompilation(
            "illegal/Interface",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            val expectedLocation = if (k2) {
                "Interface.kt:6:17"
            } else {
                "Interface.kt"
            }
            val expectedMessage = if (k2) {
                "Poko can only be applied to a class"
            } else {
                "Poko class must have a primary constructor"
            }
            assertThat(result.messages).all {
                contains(expectedLocation)
                contains(expectedMessage)
            }
        }
    }

    @Test fun `compilation of data class fails`() {
        testCompilation(
            "illegal/Data",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            val expectedLocation = if (k2) {
                "Data.kt:6:7"
            } else {
                "Data.kt"
            }
            assertThat(result.messages).all {
                contains(expectedLocation)
                contains("Poko cannot be applied to a data class")
            }
        }
    }

    @Test fun `compilation of value class fails`() {
        testCompilation(
            "illegal/Value",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            val expectedLocation = if (k2) {
                "Value.kt:6:18"
            } else {
                "Value.kt"
            }
            assertThat(result.messages).all {
                contains(expectedLocation)
                contains("Poko cannot be applied to a value class")
            }
        }
    }

    @Test fun `compilation without primary constructor fails`() {
        testCompilation(
            "illegal/NoPrimaryConstructor",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            val expectedLocation = if (k2) {
                "NoPrimaryConstructor.kt:6:13"
            } else {
                "NoPrimaryConstructor.kt"
            }
            assertThat(result.messages).all {
                contains(expectedLocation)
                contains("Poko class must have a primary constructor")
            }
        }
    }

    @Test fun `compilation without constructor properties fails`() {
        testCompilation(
            "illegal/NoConstructorProperties",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            val expectedLocation = if (k2) {
                "NoConstructorProperties.kt:6:13"
            } else {
                "NoConstructorProperties.kt"
            }
            assertThat(result.messages).all {
                contains(expectedLocation)
                contains("Poko class primary constructor must have at least one not-skipped property")
            }
        }
    }

    @Test fun `compilation of inner class fails`() {
        testCompilation(
            "illegal/OuterClass",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            val expectedLocation = if (k2) {
                "OuterClass.kt:8:11"
            } else {
                "OuterClass.kt"
            }
            assertThat(result.messages).all {
                contains(expectedLocation)
                contains("Poko cannot be applied to an inner class")
            }
        }
    }

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

    @Test fun `unknown annotation name produces expected error message`() {
        testCompilation(
            "api/Primitives",
            pokoAnnotationName = "nonexistent/ClassName",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
        ) {
            assertThat(it.messages).contains("e: Could not find class <nonexistent/ClassName>${System.lineSeparator()}")
        }
    }

    @Test fun `no builder annotation generates no builder class`() {
        testCompilation("api/Primitives") { result ->
            assertThat(result.messages)
                .doesNotContain("The Poko Builder feature")

            val builderClass = result.classLoader.tryLoadClass("api.Primitives\$Builder")
            assertThat(builderClass).isNull()
        }
    }

    @Test fun `builder annotation generates builder class`() {
        assumeTrue(k2) // FIR only works in K2

        testCompilation(
            "api/Buildable", "api/MyData",
            pokoAnnotationName = "api/MyData",
        ) { result ->
            assertThat(result.messages).all {
                contains("Buildable.kt:3:1")
                contains("The Poko Builder feature is incomplete, experimental, and private; your generated builder will not work")
            }

            val builderClass = result.classLoader.tryLoadClass("api.Buildable\$Builder")!!
            assertAll {
                assertThat(builderClass.declaredConstructors).hasSize(1)
                assertThat(builderClass.getConstructor()).isNotNull()

                val methods = builderClass.methods
                assertThat(methods.filter { it.name == "getId" }).all {
                    single().returnType().isEqualTo(String::class.java)
                }
                assertThat(methods.filter { it.name == "setId" }).all {
                    hasSize(2)
                    index(0).all {
                        returnType().isEqualTo(builderClass)
                        parameters().all {
                            hasSize(1)
                            index(0).type().isEqualTo(String::class.java)
                        }
                    }
                    index(1).all {
                        returnType().isEqualTo(Void::class.javaPrimitiveType)
                        parameters().all {
                            hasSize(1)
                            index(0).type().isEqualTo(String::class.java)
                        }
                    }
                }
            }
        }
    }

    private inline fun testCompilation(
        vararg sourceFileNames: String,
        pokoAnnotationName: String = "dev/drewhamilton/poko/Poko",
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (JvmCompilationResult) -> Unit = {}
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
        additionalTesting: (JvmCompilationResult) -> Unit = {}
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
        if (k2) {
            supportsK2 = true
        } else {
            supportsK2 = false
            languageVersion = "1.9"
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
