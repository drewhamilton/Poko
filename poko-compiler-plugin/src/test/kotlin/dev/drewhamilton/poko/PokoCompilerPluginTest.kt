package dev.drewhamilton.poko

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import java.io.File
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
            assertThat(it.messages).isEqualTo("e: Could not find class <nonexistent/ClassName>\n")
        }
    }
    //endregion

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
