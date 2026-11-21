package dev.drewhamilton.poko.runners

import dev.drewhamilton.poko.CompilerOptions
import dev.drewhamilton.poko.PokoCompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.devkit.services.TEST_COMPILER_VERSION
import org.jetbrains.kotlin.compiler.plugin.devkit.services.configurePlugin
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

fun TestConfigurationBuilder.configureTests() {
    configurePlugin(PokoCompilerPluginRegistrar())
    useConfigurators(::PokoDirectiveConfigurator)
    defaultDirectives {
        -FIR_DUMP
        -DUMP_IR
        if (TEST_COMPILER_VERSION < KotlinToolingVersion("2.4.0-Beta1"))
            LANGUAGE with "+${LanguageFeature.PropertyParamAnnotationDefaultTargetMode}"
    }
}

class PokoDirectiveConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers = listOf(PokoDirectives)
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val pokoAnnotationName = module.directives[PokoDirectives.POKO_ANNOTATION_NAME].firstOrNull()
        if (!pokoAnnotationName.isNullOrEmpty())
            configuration.put(CompilerOptions.POKO_ANNOTATION, pokoAnnotationName)
    }
}

object PokoDirectives : SimpleDirectivesContainer() {
    val POKO_ANNOTATION_NAME by stringDirective("Annotation class name")
}
