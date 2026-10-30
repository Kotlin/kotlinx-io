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
- Once CI is green, merge `develop` into `master`;
- [Create a draft](https://github.com/Kotlin/kotlinx-io/releases/new) describing the next release in GH:
  - it's more or less fine to pick up the record from the [CHANGELOG](../CHANGELOG.md), update it if
  necessary;
  - release names are usually starts with `v`, like `v0.9.10`, but there's no `v` in a git tag, like `0.9.10`.
- [Deploy](https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_KotlinxIo_DeployToCentral_DeployRunThisOne) 
  the library to Maven Central from `master` branch using the appropriate version as a `DeployVersion` parameter;
- Update version in `KOTLINX_IO_RELEASE_TAG` for [JetBrains/kotlin-web-site](https://github.com/JetBrains/kotlin-web-site/blob/master/.teamcity/BuildParams.kt#L7)
- Update version used by the Kotlin Playground in [https://github.com/JetBrains/kotlin-compiler-server](https://github.com/JetBrains/kotlin-compiler-server/blob/e2417f3a915bb671ec23101959ed1799a060d6f1/gradle/libs.versions.toml#L15)
- Set previously created draft in [kotlinx-io/releases](https://github.com/Kotlin/kotlinx-io/releases)
as the latest version;
- That's all, folks!
