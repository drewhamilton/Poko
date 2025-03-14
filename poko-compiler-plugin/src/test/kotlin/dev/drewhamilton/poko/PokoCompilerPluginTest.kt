package dev.drewhamilton.poko

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import java.io.File
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@OptIn(ExperimentalCompilerApi::class)
@RunWith(TestParameterInjector::class)
class PokoCompilerPluginTest(
    @TestParameter private val compilationMode: CompilationMode,
) {

    @JvmField
    @Rule var temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test fun `compilation of valid class succeeds`() {
        testCompilation("api/Primitives")
    }

    // TODO: Add similar test to :poko-tests after FIR is the only compilation mode
    @Test fun `compilation with value interface succeeds`() {
        assumeTrue(compilationMode == CompilationMode.K2WithFirGeneration)
        testCompilation("api/DataInterface")
    }

    @Test fun `compilation of interface fails`() {
        testCompilation(
            "illegal/Interface",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        ) { result ->
            val expectedLocation = if (compilationMode.k2) {
                "Interface.kt:6:17"
            } else {
                "Interface.kt"
            }
            val expectedMessage = if (compilationMode.k2) {
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
            val expectedLocation = if (compilationMode.k2) {
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
            val expectedLocation = if (compilationMode.k2) {
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
            val expectedLocation = if (compilationMode.k2) {
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
            val expectedLocation = if (compilationMode.k2) {
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
            val expectedLocation = if (compilationMode.k2) {
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

    @Test fun `compilation with unknown pokoPluginArg yields warning`() {
        testCompilation(
            pokoPluginArgs = "poko.nothing=value",
        ) { result ->
            val warning = "w: Ignoring unknown Poko plugin arg: poko.nothing"
            assertThat(result.messages).contains(warning)
        }
    }

    @Test fun `compilation with invalid pokoPluginArg fails`() {
        testCompilation(
            pokoPluginArgs = "poko.nothing",
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
        ) { result ->
            val error = "Invalid syntax for <poko.nothing>: must be in `key=value` property format"
            assertThat(result.messages).contains(error)
        }
    }

    @Test fun `compilation with firGenerationDeclaration arg yields warning`() {
        assumeTrue(compilationMode == CompilationMode.K2WithFirGeneration)
        testCompilation { result ->
            val warning = "w: <poko.experimental.enableFirDeclarationGeneration> resolved to " +
                "true. This experimental flag may disappear at any time."
            assertThat(result.messages).contains(warning)
        }
    }

    @Test fun `non-k2 compilation with firGenerationDeclaration enabled arg fails`() {
        assumeTrue(compilationMode == CompilationMode.NotK2)
        testCompilation(
            pokoPluginArgs = FIR_GENERATION_ENABLED_ARG,
            expectedExitCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
        ) { result ->
            val error = "Cannot use experimental Poko FIR generation with K2 disabled"
            assertThat(result.messages).contains(error)
        }
    }

    private inline fun testCompilation(
        vararg sourceFileNames: String,
        pokoAnnotationName: String = "dev/drewhamilton/poko/Poko",
        pokoPluginArgs: String? = null,
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (JvmCompilationResult) -> Unit = {}
    ) = testCompilation(
        *sourceFileNames.map { SourceFile.fromPath("src/test/resources/$it.kt") }.toTypedArray(),
        pokoAnnotationName = pokoAnnotationName,
        pokoPluginArgs = pokoPluginArgs,
        expectedExitCode = expectedExitCode,
        additionalTesting = additionalTesting
    )

    private inline fun testCompilation(
        vararg sourceFiles: SourceFile,
        pokoAnnotationName: String,
        pokoPluginArgs: String? = null,
        expectedExitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalTesting: (JvmCompilationResult) -> Unit = {}
    ) {
        val result = prepareCompilation(
            *sourceFiles,
            pokoAnnotationName = pokoAnnotationName,
            pokoPluginArgs = pokoPluginArgs,
        ).compile()
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
        pokoPluginArgs: String?,
    ) = KotlinCompilation().apply {
        workingDir = temporaryFolder.root
        compilerPluginRegistrars = listOf(PokoCompilerPluginRegistrar())
        inheritClassPath = true
        sources = sourceFiles.asList()
        verbose = false
        jvmTarget = JvmTarget.JVM_1_8.description
        if (compilationMode.k2) {
            supportsK2 = true
        } else {
            supportsK2 = false
            languageVersion = "1.9"
        }

        val commandLineProcessor = PokoCommandLineProcessor()
        commandLineProcessors = listOf(commandLineProcessor)

        @Suppress("NAME_SHADOWING") // Intentional
        val pokoPluginArgs = if (compilationMode == CompilationMode.K2WithFirGeneration) {
            listOfNotNull(pokoPluginArgs, FIR_GENERATION_ENABLED_ARG)
                .joinToString(BuildConfig.POKO_PLUGIN_ARGS_LIST_DELIMITER.toString())
        } else {
            pokoPluginArgs
        }
        pluginOptions = listOfNotNull(
            commandLineProcessor.option(CompilerOptions.ENABLED, true),
            commandLineProcessor.option(CompilerOptions.POKO_ANNOTATION, pokoAnnotationName),
            pokoPluginArgs?.let {
                commandLineProcessor.option(CompilerOptions.POKO_PLUGIN_ARGS, it)
            },
        )
    }

    private fun CommandLineProcessor.option(
        key: CompilerConfigurationKey<*>,
        value: Any?,
    ) = PluginOption(
        pluginId,
        key.toString(),
        value.toString()
    )

    private fun SourceFile.Companion.fromPath(path: String): SourceFile = fromPath(File(path))

    @Suppress("unused") // Test parameter values
    enum class CompilationMode(
        val k2: Boolean,
    ) {
        NotK2(k2 = false),
        K2WithoutFirGeneration(k2 = true),
        K2WithFirGeneration(k2 = true),
    }

    private companion object {
        private const val FIR_GENERATION_ENABLED_ARG =
            "poko.experimental.enableFirDeclarationGeneration" +
                BuildConfig.POKO_PLUGIN_ARGS_ITEM_DELIMITER +
                "true"
    }
}
