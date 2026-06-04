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

    /**
     * Configures which Poko FIR extensions run in non-CLI sessions (i.e. IDE analysis).
     *
     * CLI compiler sessions always run the full FIR plugin regardless of this setting.
     * The default is [PokoFirIdeMode.ALL], but if you have compatibility issues in
     * the IDE you can lower this to [PokoFirIdeMode.CHECKERS_ONLY] or
     * [PokoFirIdeMode.NONE] to completely disable Poko in the IDE.
     */
    public val firIdeMode: Property<PokoFirIdeMode>
}

/** Controls which Poko FIR extensions run in non-CLI sessions. */
public enum class PokoFirIdeMode {
    /** Run FIR declaration generation and checkers. */
    ALL,

    /** Run FIR checkers only, without declaration generation. Checkers are diagnostics like warnings and errors. */
    CHECKERS_ONLY,

    /** Do not run Poko FIR declaration generation or checkers. */
    NONE,
}
