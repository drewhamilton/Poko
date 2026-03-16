# Releasing

 1. If this is a breaking release, merge any PRs waiting for the next breaking release.
 2. Progress any deprecations (via PR) if applicable.
 3. Make sure you're on the latest commit on the main branch.
 4. Update CHANGELOG.md for the impending release.
 5. Change `PUBLISH_VERSION` in gradle.properties to a non-SNAPSHOT version.
 6. Update README.md for the impending release.
 7. Commit (don't push) the changes with message "Release x.y.z", where x.y.z is the new version.
 8. Tag the commit `x.y.z`, where x.y.z is the new version.
 9. Change `PUBLISH_VERSION` in gradle.properties to the next SNAPSHOT version.
10. Commit the snapshot change.
11. Push the tag and 2 commits to origin/main.
12. Wait for the "Release" Action to complete.
13. Create the release on GitHub with release notes copied from the changelog.

If steps 12 fails: drop the Sonatype repo, fix the problem, delete the incorrect tag on both local
and remote, and start over.
