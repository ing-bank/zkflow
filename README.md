[![Build Status](https://dev.azure.com/NeoZKP/zkflow/_apis/build/status/ZKFLow%20PRs%20and%20mergest%20to%20master?branchName=master)](https://dev.azure.com/NeoZKP/zkflow/_build/latest?definitionId=1&branchName=master)

## Testing best practices

We follow most of the best practices defined here: https://phauer.com/2018/best-practices-unit-testing-kotlin/.
We use some different libs for assertions and mocks, but the principles remain the same.

### Test Class lifecycle

We have configured JUnit5 so that test classes are instantiated only once. This means 'static' setup can just go in the init block or by declaring vals. If you want to isolate individual tests more, you can still have a setup method that runs before or after each test. Just annotate it with `@BeforeEach` or `@AfterEach` (JUnit4's `@Before` will no longer work).

Unfortunately this does not apply cleanup that needs to happen after all tests in a class have completed: for that you will still need a function annotated with `@AfterAll`, since there is no destroy on Kotlin classes where this could be placed.

## Static analysis

For static analysis, we use [ktlint](https://ktlint.github.io/) for code style, and [detekt](https://detekt.github.io/detekt/) for so-called mess detection (like [PMD](https://pmd.github.io/) for Java).

The goal of these checks is to ensure maintainability and readability of our code. We are not going for a beauty contest. That means that apply the following policy:

* Violations are errors and break the build
* We fail only on thing we really care about. So we prefer less rules that are breaking, over more rules that are warnings. Experience shows that long lists of warnings tend to get ignored and it is hard to spot when new warnings get added. Experience also shows that if the rules are too strict and the build fails on trivial issues, it is very demotivating and defeating the purpose.

It may be that we have too many or too strict rules at the start. It is always an option to remove or relax a rule, but always try to get agreement on this first.

On config:

Ktlint is not configurable and enforces the default Kotlin code style. Our project uses `spotless` as a wrapper, which enables auto-formatting. Any style fix that can be applied by ktlint without human intervention is automatically applied on every Gradle test run.

Detekt is very configurable, and to start with, we have chosen to use the default (community-discussed for *ages*) ruleset. We have disabled style and formatting rules because that is covered by ktlint.

### What to do when these checks fail the build?

When either ktlint or detekt fail the build, you have a few options to fix it:

1. Update your code. This is obviously the preferable option in most cases.
2. Annotate the offending code with `@Suppress("SomeRuleName")`. Be ready to explain your reasoning during code review.
3. Propose a change to the ruleset (not possible for ktlint). A mentioned above, this is always an option, but should be a last resort only when we see too many suppression annotations for a rule.

## Configuring how the tests run

If you only want to run the fast unit tests, you can run `./gradlew build` or `./gradlew test`. If you also want to run slow tests (e.g. tests that use a real Zinc ciruit, or long node driver tests), you can run `./gradlew test slowTest`.
You can mark a test as a slow test, by tagging it like so `@Tag("slow")`.

Similarly, we have `@Tag("nightly")`, which is for tests that are *so* slow, that they should only be run nightly on the CI.
If you need to run them locally, you can do `./gradlew test nightlyTest`

To run Zinc tests that use the real contract circuit faster, you can set the following system property: `MockZKP`. Setting this makes tests that normally tests against the real Zinc circuit use a mock. This can be useful for quick tests on other parts of the protocol code surrounding the circuit. Of course, always run unmocked before you submit a PR for review.

> How to set a system property? `./gradlew test slowTest -DMockZKP`.

### Which tests are `slow`

In order to decide whether a test is slow, consider the following statements:

- Tests with big circuits are definitely slow
- Test with simple comparison circuits such as for BD are not actually slow
- Test that do use a big circuit, but only run and not setup/prove/verify are also not slow

## Configuring logging during testing

To change log levels, you can make changes in the following files:

* `config/test/log4j2.xml`. This file determines how our main logger logs. Note that it is only affecting test builds. The file will have no effect on the final jar. If you really want, you can even override this file by specifying your own with the following system property: `log4j.configurationFile`.
* `config/test/logging-test.properties`. This file determines the log levels for parts of our application (mostly dependencies) that directly use java.util.logging logger. This for instancte determines how JUnit logs before our tests run.

## Verifying test coverage

To assess the test coverage, you can run `./gradlew test jacocoTestReport`. The coverage reports can be found in `build/reports/jacoco/test/html` for each module.

## Prerequisites

### Java version

This project requires Java 8. To be consistent with our CI, it is advisable to use Zulu OpenJDK 8u232b18.
On Mac, that is very easy to install and manage with [SDKMAN](https://sdkman.io/).

### Rust

You need to install the latest stable of version of Rust in order to format all circuits. Visit its [homepage](https://www.rust-lang.org/learn/get-started) for instructions.
After you install it, you need to add one extra component executing the following command:
```bash
rustup component add rustfmt
```

### Zinc

You need to install our fork of Zinc from the [Github](https://github.com/ingzkp/zinc/releases). Ask your teammates, which one you need. If you use Linux, just download already built binaries, but on the MacOS you need to build them yourself.
Clone repository and build it:
```bash
$ git clone git@github.com:ingzkp/zinc.git
$ cd zinc
$ git checkout ing-fork
$ cargo b --release
```
Built binaries will be stored in `./target/release`. Move `zargo`, `znc`, `zvm` to the directory you prefer and add it to PATH. Then you can delete sources.


### Gradle

Typically, Gradle does not require any specific changes, but you might encounter the following error during the build (path can be defferent):

```bash
Caused by: java.io.IOException: Cannot run program "zargo" (in directory "/Users/mq23re/Developer/zk-notary/prover/circuits/create"): error=2, No such file or directory
```

To fix it, run the command below from the project directory. It will stop daemon, thus it will clear cache, which can help to resolve the issue.

```bash
./gradlew --stop
```

### Our Corda fork

This project makes use of our fork of Corda maintained here: https://github.com/ingzkp/corda/tree/ing-fork
This fork is based on the latest version of Corda and will have all our proposed PRs to Corda already merged.
The artifacts for our fork are deployed to Github Packages and this project is aware of that and will be able to find Corda dependencies there

Because we use private repositories, gradle needs to be authenticated with GitHub to fetch packages. 
This is handled in repositories.gradle file. To make that work, please make sure you have the following variables 
in your shell environment: GITHUB_USERNAME & GITHUB_TOKEN. You can generate a token here:  https://github.com/settings/tokens. The username is just your GitHub username. 
If you really want to know how it works, see [here](https://help.github.com/en/packages/publishing-and-managing-packages/about-github-packages#about-tokens) and [here](https://help.github.com/en/packages/using-github-packages-with-your-projects-ecosystem/configuring-gradle-for-use-with-github-packages)

If you want to make changes to our fork (please consult with Matthijs first), you can do the following:
```bash
$ git clone https://github.com/ingzkp/corda
$ cd corda
$ git checkout ing-fork
$ git checkout -b feature/foobar
```
Then make you changes in your branch and create a PR. Please note that if you want to make these change available through
Github Packages, that you should also bump the Corda patch version (`cordaVersion`) in the file `constants.properties`.
Normally we would just use a SNAPSHOT version for this, but unfortunately Github Packages does not properly support this at this time.
That means we will just have to bump the version when we introduce a feature that we want to make available.

## A note on commit messages

For consistency, it is important that we use one standard for our commit messages. 

For wording, we use the same standard as git itself. This is taken from: https://github.com/git/git/blob/master/Documentation/SubmittingPatches

> Describe your changes in imperative mood, e.g. "make xyzzy do frotz"
> instead of "[This patch] makes xyzzy do frotz" or "[I] changed xyzzy
> to do frotz", as if you are giving orders to the codebase to change
> its behavior. 

Think of it this way: the commit message describes what will happen when a commit is applied.

For format we follow the advice from the `git commit` [manpage](https://mirrors.edge.kernel.org/pub/software/scm/git/docs/git-commit.html#_discussion):

> Though not required, it’s a good idea to begin the commit message with a single short (less than 50 character) line summarizing the change, followed by a blank line and then a more thorough description. The text up to the first blank line in a commit message is treated as the commit title, and that title is used throughout Git. 

Like other holy wars, such as tabs versus spaces, VIM vs Emacs, etc., this can be argued about. Let's not. 

## Branching strategy

In this repo, we will use Github Flow: https://guides.github.com/introduction/flow/

The aim is to have a commit history that looks like this, where feature branches always have linear history and no merge commits inside themselves.
```
* 58e7cea - (HEAD -> master, origin/master) Explicitly specify DigestServices in test
*   78b0bc4 - Merge pull request #18 from ingzkp/feature/simplify-flows
|\
| * 5328da4 - Upgrade ktlint version and import order
| * 89e8283 - Split DigestService for leaves and nodes 
| * 2f5b0bc - Add limitations doc
|/
*   dad82db - Merge pull request #17 from ingzkp/feature/simple-e2e-zkp
|\
| * 4ff33b7 - First version of e2e notarisation protocol
|/
*   ff0db31 - Merge pull request #13 from ingzkp/feature/zkwtx 
|\
| * db8d499 - Add ZKProverTx, ZKVerifierTx and JSON serialization 
|/
* 75b47d0 - Change Github Actions cache key 
```
One of the challenges in creating a meaningful history like this, is in keeping feature branches up to date with master without polluting their history with merge commits, and therefore polluting master when they are merged to master. Keeping a feature branch up to date by merging master into it regularly causes lots of merge commits from master like this This is not what we want to see:
```
* 58e7cea - (HEAD -> master, origin/master) Explicitly specify DigestServices in test
*   78b0bc4 - Merge pull request #18 from ingzkp/feature/simplify-flows
|\
| * 5328da4 - Upgrade ktlint version and import order
| * 12ab43c - Merge branch 'master' of github.com
| |\
| | * 98gf55c - Foo bar Baz
| | |
| * | 2f5b0bc - Add limitations doc
|/ /
|/   
* dad82db - Merge pull request #17 from ingzkp/feature/simple-e2e-zkp
```
There are two ways to prevent this. The best option is changing your development workflow to use regular rebasing on master as explained here: https://www.atlassian.com/git/tutorials/merging-vs-rebasing. Alternatively, if you really hate rebasing and especially the merge conflicts it can cause, you can also clean up your feature branch by using only one final rebase when you are ready to merge your branch into master: `git rebase -i origin/master`. Rebasing in itself already makes your feature branch history linear, but when you do it interactively, you can then `fixup` commits that you may not want to end up visibly in master, or `reword` bad commit messages.

## How to release

A release can be made by creating a tag with the pattern `release/VERSION`. This will trigger a github action that will publish the jar file to the github maven repository, with version `VERSION`.

### How it works

In the github workflow `on-tag-publish.yml`, the version is parsed from the git tag, by removing the prefix `release/`. This version is set in the environment variable `GIT_RELEASE_VERSION`, and passed to gradle using the `-Pversion=$GIT_RELEASE_VERSION`.

For more information checkout the following files

- [on-tag-publish.yml](.github/workflows/on-tag-publish.yml)
