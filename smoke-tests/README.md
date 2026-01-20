Tests aimed to verify that all artifacts are published correctly
and there are no dependency-related issues.

There are two sets of tests: one targeting Gradle projects and another targeting Maven projects.
Each set could be invoked independently via `verifyGradleProjects` and `verifyMavenProjects` tasks
correspondingly. Both tasks are aggregated by the `smokeTest` task.

Note that `check` and `test` tasks are no-op.

Tests could be executed against arbitrary `kotlinx-io` version. The version could be set using
`smokeTest.kotlinxIoVersion` property. If the property is unset or has a blank value, current project
version will be used instead. In that case, local maven repo will be used and publication tasks
will be executed before any tests.

For projects published to a staging Sonatype repository it's possible to specify repository URI 
using `smokeTest.stagingRepository` property.

### How to run

`./gradlew :kotlinx-io-smoke-tests:smokeTest -PsmokeTest.kotlinxIoVersion=0.5.3-test -PsmokeTest.stagingRepository=file:///tmp/buildRepo`
