package dev.drewhamilton.poko

import java.lang.reflect.Method
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@ExperimentalCompilerApi
internal fun CompilerPluginRegistrar.ExtensionStorage.registerIrExtensionCompat(
    extension: IrGenerationExtension,
) {
    try {
        IrGenerationExtension.registerExtension(extension)
    } catch (_: ClassCastException) {
        javaClass.registerExtensionMethod.invoke(
            this,
            IrGenerationExtension.Companion,
            extension,
        )
    }
}

@ExperimentalCompilerApi
internal fun CompilerPluginRegistrar.ExtensionStorage.registerFirExtensionCompat(
    extension: FirExtensionRegistrarAdapter,
) {
    try {
        FirExtensionRegistrarAdapter.registerExtension(extension)
    } catch (_: ClassCastException) {
        javaClass.registerExtensionMethod.invoke(
            this,
            FirExtensionRegistrarAdapter.Companion,
            extension,
        )
    }
}

@ExperimentalCompilerApi
private val Class<CompilerPluginRegistrar.ExtensionStorage>.registerExtensionMethod: Method
    get() = methods.single {
        it.name == "registerExtension" &&
            it.parameters.size == 2 &&
            it.parameters[0].type.name == "org.jetbrains.kotlin.extensions.ExtensionPointDescriptor" &&
            it.parameters[1].type == Any::class.java
    }
