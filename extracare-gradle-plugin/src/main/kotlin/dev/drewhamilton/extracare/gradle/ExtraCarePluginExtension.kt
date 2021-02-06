package dev.drewhamilton.extracare.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ExtraCarePluginExtension @Inject constructor(objects: ObjectFactory) {

    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
        .convention(true)

    val dataApiAnnotation: Property<String> = objects.property(String::class.java)
        .convention(ExtraCareGradlePlugin.DEFAULT_DATA_API_ANNOTATION)
}
