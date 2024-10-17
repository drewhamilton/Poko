dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))

            //region Duplicated in ../settings.gradle
            fun String.nullIfBlank(): String? = if (isNullOrBlank()) null else this

            // Compile sample project with different Kotlin version than Poko, if provided:
            val kotlinVersionOverride = System.getenv()["poko_sample_kotlin_version"]?.nullIfBlank()
            kotlinVersionOverride?.let { kotlinVersion ->
                version("kotlin", kotlinVersion)
            }
            //endregion
        }
    }
}
