package dev.drewhamilton.poko.gradle

import org.gradle.api.provider.Property

public interface PokoPluginExtension {
    public val enabled: Property<Boolean>

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
    public val pokoAnnotation: Property<String>
}
