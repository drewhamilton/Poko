# Releasing

 1. If this is a breaking release, merge any PRs waiting for the next breaking release.
 2. Make sure you're on the latest commit on the main branch.
 3. Update CHANGELOG.md for the impending release.
 4. Change `PUBLISH_VERSION` in gradle.properties to a non-SNAPSHOT version.
 5. Update README.md for the impending release.
 6. Commit (don't push) the changes with message "Release x.y.z", where x.y.z is the new version.
 7. Tag the commit `x.y.z`, where x.y.z is the new version.
 8. Change `PUBLISH_VERSION` in gradle.properties to the next SNAPSHOT version.
 9. Commit the snapshot change.
10. Push the tag and 2 commits to origin/main.
11. Wait for the "Release" Action to complete.
12. Create the release on GitHub with release notes copied from the changelog.

If steps 11 fails: drop the Sonatype repo, fix the problem, delete the incorrect tag on both local
and remote, and start over.
