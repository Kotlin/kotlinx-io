# kotlinx-io release check list

`kotlinx-io` release process consists of several partially automated steps.

This document is aimed for engineers responsible for releasing the library and should read as an 
overview of how things are done usually rather than a rigid script that should be followed strictly.

## TODO

Some steps are not fully automated, some steps fail (like the smoke tests) even though everything
is fine. It all should be fixed sooner or later.

## Checklist

As it is described in [contibuting guidelines](../CONTRIBUTING.md), 
the developent takes place in `develop` branch
and `master` branch contains sources for a recently released version.

The following steps are usually performed:
- Ensure all tests pass in CI for the `develop` branch;
- Update [README](../README.md) instructions on how to use the library with a new version;
- Add a note describing what will be released to the [CHANGELOG](../CHANGELOG.md);
- Bump up the current version in [gradle.properties](../gradle.properties);
- Commit changes in `README`, `CHANGELOG` and `gradle.properties` into the `develop` branch;
- [Deploy](https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_KotlinxIo_DeployRunThisOne)
a pre-release version of the library into [Sonatype](http://oss.sonatype.org) staging repository,
close it and [run smoke tests](https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_KotlinxIo_DeploymentSmokeTest) 
with this pre-release version;
- If smoke tests passed, merge the `develop` branch into `master`, wait until a successful test run in CI;
- Drop the pre-release version from Sonatype;
- [Create a draft](https://github.com/Kotlin/kotlinx-io/releases/new) describing the next release in GH:
  - it's more or less fine to pick up the record from the [CHANGELOG](../CHANGELOG.md), update it if
  necessary;
  - release names are usually starts with `v`, like `v0.9.10`, but there's no `v` in a git tag, like `0.9.10`.
- Deploy the release version, close the repo, run smoke tests with it and if tests passed, release the repository.
- Update version in `KOTLINX_IO_RELEASE_TAG` for [JetBrains/kotlin-web-site](https://github.com/JetBrains/kotlin-web-site/blob/master/.teamcity/BuildParams.kt#L7)
- Set previously created draft in [kotlinx-io/releases](https://github.com/Kotlin/kotlinx-io/releases)
as the latest version;
- That's all, folks!
