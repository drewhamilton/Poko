package dev.drewhamilton.poko.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class PokoPluginExtension @Inject constructor(objects: ObjectFactory) {

    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
        .convention(true)

    val pokoAnnotation: Property<String> = objects.property(String::class.java)
        .convention(DEFAULT_POKO_ANNOTATION)
}
