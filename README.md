# Careful

> Using Kotlin types whose properties will change over time in public API requires extra care to
> maintain source and binary compatibility as well as an idiomatic API for each language.

— Jake Wharton,
  [Public API challenges in Kotlin](https://jakewharton.com/public-api-challenges-in-kotlin/)

Careful is a Kotlin compiler plugin that makes writing data classes for public APIs easy. Like with
actual data classes, all you have to do is provide members in your class's constructor. Then give it
the `@Careful` annotation and enjoy the generated `toString`, `equals`, `hashCode`, builder class
for Java consumers, and DSL initializer for Kotlin consumers.

Careful is a work in progress. It's not published as an artifact yet.

## Use

Write a simple class:
```kotlin
@Careful class Simple(
    val int: Int,
    val requiredString: String,
    val optionalString: String?
)
```

And enjoy the benefits of a readable `toString` and working `equals` and `hashCode`. Unlike data
classes, no `copy` or `componentN` functions are generated.

## To-do:
* Publish to Maven Central
* Make the repo public
* Handle arrays in `equals`?
* Generate the inner Builder class
* Mark the constructor as private
* Generate the top-level DSL initializer
* Add robust testing
* Add robust compiler messages for unsupported cases
* Write an IDE plugin?
* Multiplatform support?
