package api

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class MyData {
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.CLASS)
    annotation class Builder
}
