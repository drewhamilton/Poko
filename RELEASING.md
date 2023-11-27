# Releasing

 1. Make sure you're on the latest commit on the main branch.
 2. Change `VERSION_NAME` in gradle.properties to a non-SNAPSHOT version.
 3. Update README.md for the impending release.
 4. Update CHANGELOG.md for the impending release.
 5. Commit (don't push) the changes with message "Release x.y.z", where x.y.z is the new version.
 6. Tag the commit `x.y.z`, where x.y.z is the new version.
 7. Change `VERSION_NAME` in gradle.properties to the next SNAPSHOT version.
 8. Commit the snapshot change.
 9. Push the tag and 2 commits to origin/main.
10. Wait for the "Release" Action to complete.
11. Create the release on GitHub with release notes copied from the changelog.

If steps 10 fails: drop the Sonatype repo, fix the problem, commit, and start again at step 6.
