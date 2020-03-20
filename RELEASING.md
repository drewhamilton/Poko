# Releasing

1. Make sure you're on the master branch.
2. Change `publish_version` in gradle.properties. 
3. Update CHANGELOG.md for the impending release.
4. `./gradlew assemble && ./gradlew publishReleasePublicationToMavenCentralRepository && cd extracare-gradle-plugin && ./gradlew assemble && ./gradlew publishReleasePublicationToMavenCentralRepository && cd ..`
5. Visit [Sonatype Nexus](https://oss.sonatype.org/#stagingRepositories) and promote the artifacts.
6. Commit and push the release changes to master.
7. Create the release tag on GitHub.
