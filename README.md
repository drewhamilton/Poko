# Poko
![](https://github.com/drewhamilton/Poko/workflows/CI/badge.svg?branch=main)

Poko is a Kotlin compiler plugin that makes writing and maintaining data model classes for public
APIs easy. Like with normal Kotlin data classes, all you have to do is provide members in your
class's constructor. Then give it the `@Poko` annotation and enjoy the generated `toString`,
`equals`, and `hashCode`. (Builder class for Java consumers and DSL initializer for Kotlin consumers
to be added.)

Poko supports both IR and non-IR compilation.

## Use
Mark your class as a `@Poko class` instead of a `data class`:
```kotlin
@Poko class MyData(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
)
```

And enjoy the benefits of a readable `toString` and working `equals` and `hashCode`. Unlike normal
data classes, no `copy` or `componentN` functions are generated.

### Annotation
By default, the `dev.drewhamilton.poko.Poko` annotation is used to mark classes for Poko generation.
If you prefer, you can create a different annotation and supply it to the Gradle  plugin.

```groovy
apply plugin: 'dev.drewhamilton.poko'
poko {
  pokoAnnotation.set 'com.example.MyDataAnnotation'
}
```

### Download

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.drewhamilton.poko/poko-compiler-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.drewhamilton.poko/poko-compiler-plugin)

Poko is available on Maven Central. It is experimental, and the API may undergo breaking changes
before version 1.0.0. Kotlin Compiler plugins in general are experimental and new versions of Kotlin
might break something in this compiler plugin.

Since the Kotlin compiler has had frequent breaking changes, different versions of Kotlin are
exclusively compatible with specific versions of Poko.

| Kotlin version  | Poko version | Extra Care version |
| --------------- | ------------ | ------------------ |
| 1.4.30          | 0.7.1        | 0.6.0              |
| 1.4.20 – 1.4.21 | N/A          | 0.5.0              |
| 1.4.0 – 1.4.10  | N/A          | 0.3.1              |
| 1.3.72          | N/A          | 0.2.4              |

Snapshots of the development version are available in [Sonatype's Snapshots
repository](https://oss.sonatype.org/#view-repositories;snapshots~browsestorage).

To use Poko, include the following in your Gradle dependencies:
```groovy
buildscript {
    dependencies {
        classpath "dev.drewhamilton.poko:poko-gradle-plugin:$version"
    }
}

// Per module:
apply plugin: 'dev.drewhamilton.poko'
```

### Extra Care

Prior to version 0.7.0, this library was called "Extra Care".

> Using Kotlin types whose properties will change over time in public API requires extra care to
> maintain source and binary compatibility as well as an idiomatic API for each language.

— Jake Wharton,
[Public API challenges in Kotlin](https://jakewharton.com/public-api-challenges-in-kotlin/)

Updating from a previous version of Extra Care to Poko will require changes to your Gradle scripts.
You can continue to use Extra Care's `@DataApi` annotation if desired.

```groovy
// Replace this:
apply plugin: 'dev.drewhamilton.extracare'

// With this:
apply plugin: 'dev.drewhamilton.poko'
poko {
  // Add this if you want to use the old annotation
  // Skip this if you want to use the new default Poko annotation
  pokoAnnotation = 'dev.drewhamilton.extracare.DataApi'
}
``` 

## To-do
* Generate the inner Builder class
* Propagate the constructor default values to the Builder 
* Mark the constructor as private
* Generate the top-level DSL initializer
* Write an IDE plugin?
* Multiplatform support?

## License
```
Copyright 2020 Drew Hamilton

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
