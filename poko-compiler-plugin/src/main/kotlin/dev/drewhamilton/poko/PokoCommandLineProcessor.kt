package dev.drewhamilton.poko

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(CommandLineProcessor::class)
@ExperimentalCompilerApi
public class PokoCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String get() = BuildConfig.COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = CompilerOptions.ENABLED.toString(),
            valueDescription = "<true|false>",
            description = "",
            required = false,
        ),
        CliOption(
            optionName = CompilerOptions.POKO_ANNOTATION.toString(),
            valueDescription = "Annotation class name",
            description = "",
            required = false,
        ),
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ): Unit = when (option.optionName) {
        CompilerOptions.ENABLED.toString() ->
            configuration.put(CompilerOptions.ENABLED, value.toBoolean())
        CompilerOptions.POKO_ANNOTATION.toString() ->
            configuration.put(CompilerOptions.POKO_ANNOTATION, value)
        else ->
            throw IllegalArgumentException("Unknown plugin option: ${option.optionName}")
    }
}
