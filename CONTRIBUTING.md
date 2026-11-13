# Contributing Guidelines

One can contribute to the project by reporting issues or submitting changes via pull request.

## LLM policy

Sending us LLM output is only allowed for automating the routine in trivial cases,
when there is only a single obvious way to do something.
This is not limited to code: any discussions, such as pull request descriptions or issue comments,
are subject to the same restriction.

Some examples of trivial cases:

- Implementing obvious refactorings, like replacing `import org.junit.Test` with `import kotlin.test.Test`.
- Generating test cases for function overloads after having written a test suite for one overload manually.
- Listing differences between large arrays of difficult-to-parse data.
- When filing an issue, minifying a reproducer, a memory dump or a stack trace.

## Reporting issues

Please use [GitHub issues](https://github.com/Kotlin/kotlinx-io/issues) for filing feature requests and bug reports.

Questions about usage and general inquiries are better suited for the [Discussions](https://github.com/Kotlin/kotlinx-io/discussions), StackOverflow, or the [#io](https://slack-chats.kotlinlang.org/c/io) channel in KotlinLang Slack.

## Submitting changes

Submit pull requests [here](https://github.com/Kotlin/kotlinx-io/pulls).
However, please keep in mind that maintainers will have to support the resulting code of the project,
so do familiarize yourself with the following guidelines.

* All development (both new features and bug fixes) is performed in the `develop` branch.
    * The `master` branch contains the sources of the most recently released version.
    * Base your PRs against the `develop` branch.
    * The `develop` branch is pushed to the `master` branch during release.
    * Documentation in markdown files can be updated directly in the `master` branch,
      unless the documentation is in the source code, and the patch changes line numbers.
* If you make any code changes:
    * Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html).
        * Use 4 spaces for indentation.
        * Use imports with '*'.
    * [Build the project](#building) to make sure it all works and passes the tests.
* If you intend to make a non-trivial change (that is, if there is more than one way to do it):
  * Do not use an LLM to author the code.
  * Make sure there is an open issue about it.
* If you fix a bug:
    * Write the test that reproduces the bug.
    * Fixes without tests are accepted only in exceptional circumstances if it can be shown that writing the
      corresponding test is too hard or otherwise impractical.
    * Follow the style of writing tests that is used in this project:
      name test functions as `testXxx`. Don't use backticks in test names.
* Comment on the existing issue if you want to work on it. Ensure that the issue not only describes a problem, but also describes a solution that has received positive feedback. Propose a solution if none has been suggested.

## Building

This library is built with Gradle.

* Run `./gradlew build` to build. It also runs all the tests.
* Run `./gradlew <module>:check` to test the module you are looking at to speed
  things up during development.
* Run `./gradlew <module>:jvmTest` to perform only the fast JVM tests of a multiplatform module.

You can import this project into IDEA, but you have to delegate build actions
to Gradle (in Preferences -> Build, Execution, Deployment -> Build Tools -> Gradle -> Build and run).

### Updating the public API dump

* Use the [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator/blob/master/README.md) for updates to public API:
    * Run `./gradlew updateLegacyAbi` to update API index files.
    * Commit the updated API indexes together with other changes.
