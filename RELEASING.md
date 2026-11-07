# Releasing

Releases go to Maven Central as `io.github.kyrylost:smartstatusbar` from the `Release` workflow.

## One-time setup

1. Sign in to [central.sonatype.com](https://central.sonatype.com) with the `kyrylost` GitHub account. The `io.github.kyrylost` namespace is verified with it.
2. Generate a user token (Account → Generate User Token).
3. Create a GPG key and publish its public part:
   ```sh
   gpg --full-generate-key
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --export-secret-keys --armor <KEY_ID>   # value of SIGNING_KEY
   ```
4. Add the repository secrets `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` (the token), `SIGNING_KEY` (the armored secret key) and `SIGNING_KEY_PASSWORD`.

## Each release

1. Set `VERSION_NAME` in `gradle.properties` and move the changes under its heading in `CHANGELOG.md`.
2. Commit, then tag and push: `git tag v<version> && git push origin v<version>`.
3. The workflow publishes and releases the deployment, then creates a GitHub release with that version's
   section of `CHANGELOG.md` as notes and the sample APK attached. Versions cannot be changed or removed
   from Maven Central once released.

To try the artifacts first, `./gradlew :smartstatusbar:publishToMavenLocal` publishes them unsigned to `~/.m2`.
