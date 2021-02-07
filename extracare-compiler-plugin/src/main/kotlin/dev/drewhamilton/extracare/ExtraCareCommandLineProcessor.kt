package dev.drewhamilton.extracare

import com.google.auto.service.AutoService
import dev.drewhamilton.extracare.info.ArtifactInfo
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(CommandLineProcessor::class)
class ExtraCareCommandLineProcessor : CommandLineProcessor {

    override val pluginId = ArtifactInfo.COMPILER_PLUGIN_ARTIFACT

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(CompilerOptions.ENABLED.toString(), "<true|false>", "", required = true),
        CliOption(CompilerOptions.DATA_API_ANNOTATION.toString(), "Annotation class name", "", required = true),
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option.optionName) {
        CompilerOptions.ENABLED.toString() -> configuration.put(CompilerOptions.ENABLED, value.toBoolean())
        CompilerOptions.DATA_API_ANNOTATION.toString() -> configuration.put(CompilerOptions.DATA_API_ANNOTATION, value)
        else -> throw IllegalArgumentException("Unknown plugin option: ${option.optionName}")
    }
}
