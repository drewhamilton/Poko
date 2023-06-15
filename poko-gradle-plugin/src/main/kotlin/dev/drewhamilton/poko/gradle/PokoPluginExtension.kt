package dev.drewhamilton.poko.gradle

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

public abstract class PokoPluginExtension @Inject constructor(objects: ObjectFactory) {

    public val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
        .convention(true)

    /**
     * Define a custom Poko marker annotation. The poko-annotations artifact won't be automatically
     * added as a dependency if a different annotation is defined.
     *
     * Note that this must be in the format of a string where packages are delimited by `/` and
     * classes by `.`, e.g. `com/example/Nested.Annotation`.
     *
     * Note that this only affects the main Poko annotation. Additional Poko annotations, such as
     * `@ArrayContentBased`, are not customizable. If a custom Poko marker annotation is defined
     * _and_ additional Poko annotations are used, the poko-annotations artifact must be manually
     * added as an `implementation` dependency.
     */
    public val pokoAnnotation: Property<String> = objects.property(String::class.java)
        .convention(DEFAULT_POKO_ANNOTATION)
}
