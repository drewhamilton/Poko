package dev.drewhamilton.poko.gradle

import dev.drewhamilton.poko.gradle.BuildConfig.DEFAULT_POKO_ANNOTATION
import dev.drewhamilton.poko.gradle.BuildConfig.DEFAULT_POKO_ENABLED
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

public abstract class PokoPluginExtension @Inject constructor(objects: ObjectFactory) {

    public val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
        .convention(DEFAULT_POKO_ENABLED)

    /**
     * Define a custom Poko marker annotation. The poko-annotations artifact won't be automatically
     * added as a dependency if a different annotation is defined.
     *
     * Note that this must be in the format of a string where packages are delimited by `/` and
     * classes by `.`, e.g. `com/example/Nested.Annotation`.
     *
     * Note that this affects the main Poko annotation and any nested annotations, such as
     * `@Poko.ReadArrayContent` and `@Poko.Skip`.
     */
    public val pokoAnnotation: Property<String> = objects.property(String::class.java)
        .convention(DEFAULT_POKO_ANNOTATION)
}
