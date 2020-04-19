# Releasing

1. Make sure you're on the master branch.
2. Change `publish_version` in gradle.properties. 
3. Update CHANGELOG.md for the impending release.
4. Clean: `./gradlew clean  && cd extracare-gradle-plugin && ./gradlew clean && cd ..`
5. Publish: `./gradlew assemble && ./gradlew publishReleasePublicationToMavenCentralRepository && cd extracare-gradle-plugin && ./gradlew assemble && ./gradlew publishReleasePublicationToMavenCentralRepository && cd ..`
6. Visit [Sonatype Nexus](https://oss.sonatype.org/#stagingRepositories). Delete the Gradle plugin
   maven-metadata.xml sha256 and sha512 artifacts. Close and release the staging repository.
7. Commit and push the release changes to master.
8. Create the release tag on GitHub.
