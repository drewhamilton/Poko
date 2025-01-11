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

    override val pluginId: String = BuildConfig.COMPILER_PLUGIN_ARTIFACT

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
        CliOption(
            optionName = CompilerOptions.POKO_PLUGIN_ARGS.toString(),
            valueDescription = "",
            description = "Additional Poko compiler plugin arguments",
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
        CompilerOptions.POKO_PLUGIN_ARGS.toString() ->
            configuration.put(CompilerOptions.POKO_PLUGIN_ARGS, value.split(','))
        else ->
            throw IllegalArgumentException("Unknown plugin option: ${option.optionName}")
    }
}
