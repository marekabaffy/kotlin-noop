# Releasing

Pushing a version tag runs [the release workflow](.github/workflows/release.yml). A release on Maven Central can never
be changed or deleted, so a mistake needs a new version.

## Setup

The workflow reads these secrets from the `release` GitHub environment, which only tags can use:

| Secret                 | Value                                               |
|------------------------|-----------------------------------------------------|
| `CENTRAL_USERNAME`     | Central Portal user token username                  |
| `CENTRAL_PASSWORD`     | Central Portal user token password                  |
| `SIGNING_KEY`          | ASCII-armored private GPG key                       |
| `SIGNING_KEY_PASSWORD` | Passphrase of that key                              |

The public half of the key must be on a key server, because Central checks signatures against it.

## Steps

1. Merge a PR that moves the `Unreleased` notes in `CHANGELOG.md` under a `## <version>` heading.
2. Tag that commit on `main` and push the tag, with no `v` prefix:

   ```shell
   git tag 0.1.0
   git push origin 0.1.0
   ```

3. Wait for the workflow. It tests the signed jars, uploads them, and creates a draft GitHub Release.
4. Inspect the deployment on the [Central Portal](https://central.sonatype.com/publishing/deployments), then click
   **Publish**. Clicking **Drop** instead discards it without releasing anything.
5. Once the version is on [Maven Central](https://repo1.maven.org/maven2/com/marekabaffy/kotlinnoop/), publish the draft
   GitHub Release and update the versions in the README.

If the workflow fails before uploading, nothing was released. Fix the problem on `main`, then delete the tag and push it
again on the new commit.
