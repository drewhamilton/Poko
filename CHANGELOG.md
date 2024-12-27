# Changelog

## 0.18.2
_2024-12-27_

Add compatibility with Kotlin 2.1.20-Beta1.

## 0.18.1
_2024-12-13_

Add ability to skip individual properties with `@Poko.Skip`. These properties will be omitted from
all generated functions. This feature is experimental and requires opt-in via `@SkipSupport`.

Replace `@ArrayContentBased` with `@Poko.ReadArrayContent`. Add a deprecated `typealias` for the
former to aid migration.

Add support for use optional property-level features with custom Poko annotation. Nested annotations
with the same name as the optional feature, such as `@MyData.ReadArrayContent` and
`@MyData.Skip`, will be respected.

Fix issue with FIR checkers. K2-enabled IDEs should now highlight Poko errors and warnings in
consumer source code.

## 0.18.0
_2024-12-2_

Compile with Kotlin 2.1.0.

## 0.17.2
_2024-10-18_

Compile with Kotlin 2.0.21. Implement compatibility with Kotlin 2.0.0 – 2.1.0-Beta2.

## 0.17.1
_2024-09-17_

Automatically add the Poko annotation dependency to test fixtures when they are enabled on JVM
projects.

## 0.17.0
_2024-08-22_

Compile with Kotlin 2.0.20. Support Java 22.

## 0.16.0
_2024-05-28_

Compile with Kotlin 2.0.0. The syntax for custom annotation references now uses `/` to denote
packages, e.g. `com/example/MyAnnotationClass`.

Deprecate `@ArrayContentSupport` annotation and stop requiring it for using `@ArrayContentBased`
annotation.

### 0.16.0-beta01
_2024-05-20_

Compile with Kotlin 2.0.0-RC3. The syntax for custom annotation references now uses `/` to denote
packages, e.g. `com/example/MyAnnotationClass`.

## 0.15.3
_2024-05-19_

Support Android native targets and Wasm-WASI target for Kotlin Multiplatform. Implement error checks
in FIR to support automatic IDE warnings in future IntelliJ releases.

Compile with Kotlin 1.9.24. Use Node v22 for Wasm and JS targets. Compile released artifacts with
`-Xjdk-release` flag to ensure JVM 1.8 compatibility.

## 0.15.2
_2024-01-02_

Support Wasm-JS target for Kotlin Multiplatform.

Compile with Kotlin 1.9.22. Support Java 21.

## 0.15.1
_2023-11-27_

Compile with Kotlin 1.9.20. Support compilation with K2. Support Java 20.

Optimize JS `equals` implementation as well as bytecode for `Int` and `UInt` properties.

## 0.15.0
_2023-08-09_

Support Kotlin Multiplatform.

Revert `poko-annotations` to an `implementation` dependency, because Kotlin/Native doesn't support
`compileOnly`. The annotations are still source-retention, so the end-consumer's app binary can
strip the annotations from the classpath during shrinking.

Update the compiler plugin to be enabled by default and to use the default annotations by default.
This brings the compiler plugin into alignment with the Gradle plugin.

## 0.14.0
_2023-07-06_

Compile with Kotlin 1.9.0.

Include default `poko-annotations` dependency as `compileOnly`, and convert `@Poko` annotation from
binary retention to source retention. This makes Poko invisible to consumers of Poko-consuming
libraries.

Change the Kotlin Gradle plugin to a `compileOnly` dependency of the Poko Gradle plugin, allowing
consumers to use older versions of Kotlin for their libraries than Poko uses (assuming compatible
versions of the Kotlin compiler API).

## 0.13.1
_2023-06-20_

Add experimental `@ArrayContentBased` annotation. This annotation can be applied to `@Poko` class
array properties to ensure that the array's content is considered for `equals`, `hashCode`, and
`toString`. (By default, array content is not considered.)

Compile with Kotlin 1.8.22.

## 0.13.0
_2023-04-03_

Support Kotlin 1.8.20.

## 0.12.0
_2023-03-21_

Support Kotlin 1.8.0 and 1.8.10. Support Java 19.

The `pokoAnnotation` configuration property's format changes; package names are now separated by `/`
instead of `.`. For example, an inner annotation class definition would look like
`"com/example/MyClass.Inner"`.

The primary entry point is now `PokoCompilerPluginRegistrar`, replacing `PokoComponentRegistrar`.
This change is transparent to consumers of the Gradle plugin.

Automatic inclusion of the legacy `@dev.drewhamilton.extracare.DataApi` artifact is removed. The
consumer can still manually depend on
`implementation("dev.drewhamilton.extracare:data-api-annotations:0.6.0")` if desired.

## 0.11.0
_2022-06-13_

Support Kotlin 1.7.0. Support Java 18.

Drop support for non-IR compilation (which does not exist in Kotlin 1.7.0).

## 0.10.0
_2022-04-15_

Support Kotlin 1.6.20.

## 0.9.0
_2021-12-04_

Support Kotlin 1.6.0.

## 0.8.1
_2021-06-04_

Publish a Gradle plugin marker ([#54](https://github.com/drewhamilton/Poko/issues/54)). Compile with
Kotlin 1.5.10.

## 0.8.0
_2021-05-18_

Support Kotlin 1.5.0. Support Java 16.

## 0.7.4
_2021-04-28_

Change the default `poko-annotations` dependency to a runtime dependency when used, rather than a
compile-only dependency. This ensures it is available in alternate configurations, like JVM test and
Android test configurations.

## 0.7.3
_2021-03-27_

Fix another bug that broke compatibility with Jetpack Compose. Poko is now compatible with Jetpack
Compose 1.0.0-beta03 and Android Gradle Plugin 7.0.0-alpha12.

## 0.7.2
_2021-03-21_

Enforce a minimum of one property in the primary constructor of a Poko class. Previously,
compilation would succeed but could cause failures down the line with unclear error messages.

## 0.7.1
_2021-02-24_

Fix a bug that broke compatibility with Jetpack Compose. Poko is now compatible with Jetpack Compose
1.0.0-alpha12.

## 0.7.0
_2021-02-08_

Rename the library from "Extra Care" to "Poko". The new artifact names are `poko-compiler-plugin`,
`poko-gradle-plugin`, and `poko-annotations`. The legacy `dev.drewhamilton.extracare.DataApi`
is still supported if specified.

## 0.6.0
_2021-02-06_

Support Kotlin 1.4.30.

Add the ability to specify a custom annotation in place of the default annotation to trigger Extra
Care generation on annotated classes.

## 0.5.0
_2021-01-10_

Support compilation in projects where IR compilation is enabled.

Disable support for `inner` and `inline` classes, which are both also not supported by the `data`
keyword.

Add an `extraCare` Gradle extension, where the compiler plugin can be explicitly enabled or
disabled.

## 0.4.0
_2021-01-01_

Support Kotlin 1.4.20 and 1.4.21.

## 0.3.1 (patched to 0.2.4)
_2020-09-29_

Add code documentation to `@DataApi`.

## 0.3.0
_2020-08-19_

Update to Kotlin 1.4.0.

## 0.2.3
_2020-05-04_

Fix `equals` and `hashCode` generation for `long` member types.

## 0.2.2
_2020-05-02_

Allow explicit `toString`, `equals`, and `hashCode` declarations in `@DataApi` classes. When one of
these is declared explicitly, that function will not be generated by Extra Care.

## 0.2.1
_2020-04-24_

Publish with Gradle module metadata.

## 0.2.0
_2020-04-21_

Change the Gradle plugin ID so applying the plugin is less verbose:
```groovy
apply plugin: 'dev.drewhamilton.extracare'
```

## 0.1.3
_2020-04-19_

Fix `equals` and `hashCode` generation for `float` and `double` member types. Update to Kotlin
1.3.72.

Fix release process bug preventing changes from landing in 0.1.1 and 0.1.2.

## 0.1.2
_2020-04-19_

No changes due to release process bug.

~~Fix `equals` and `hashCode` generation for `float` and `double` member types. Update to Kotlin
1.3.72.~~

## 0.1.1
_2020-03-25_

No changes due to release process bug.

~~Update to Kotlin 1.3.71.~~

## 0.1.0
_2020-03-20_

Initial release. Create a `@DataApi` class with `equals`, `hashCode`, and `toString` generated (but
no `copy` or `componentN`).
