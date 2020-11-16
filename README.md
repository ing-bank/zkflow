![Build](https://github.com/ingzkp/zk-notary/workflows/Fast%20and%20slow%20tests%20for%20PRs%20and%20merges%20to%20master/badge.svg?branch=master)

## Static analysis

For static analysis, we use [ktlint](https://ktlint.github.io/) for code style, and [detekt](https://detekt.github.io/detekt/) for so-called mess detection (like [PMD](https://pmd.github.io/) for Java).

The goal of these checks is to ensure maintainability and readability of our code. We are not going for a beauty contest. That means that apply the following policy:

* Violations are errors and break the build
* We fail only on thing we really care about. So we prefer less rules that are breaking, over more rules that are warnings. Experience shows that long lists of warnings tend to get ignored and it is hard to spot when new warnings get added. Experience also shows that if the rules are too strict and the build fails on trivial issues, it is very demotivating and defeating the purpose.

It may be that we have too many or too strict rules at the start. It is always an option to remove or relax a rule, but always try to get agreement on this first.

On config:

Ktlint is not configurable and enforces the default Kotlin code style. Our project uses `spotless` as a wrapper, which enables auto-formatting. Any style fix that can be applied by ktlint without human intervention is automatically applied on every Gradle test run.

Detekt is very configurable, and to start with, we have chosen to use the default (community-discussed for *ages*) ruleset. We have disabled style and formatting rules because that is covered by ktlint.

### What to do when these check fail the build?

When either ktlint or detekt fail the build, you have a few options to fix it:

1. Update your code. This is obviously the preferable option in most cases.
2. Annotate the offending code with `@Suppress("SomeRuleName")`. Be ready to explain yourself during code review.
3. Propose a change to the ruleset (not possible for ktlint). A mentioned above, this is always an option, but should be a last resort only when we see too many suppression annotations for a rule.

## Configuring how the tests run

If you only want to run the fast unit tests, you can run `./gradlew build` or `./gradlew test`. If you also want to run the slow tests (e.g. the ones that test the real Zinc ciruit), you can run `./gradlew test slowTest`.
You can mark a test as a slow test, by tagging it like so `@Tag("slow")`.

To run the slow tests, but faster, you can set the following system property: `MockZKP`. Setting this makes tests that normally tests against the real Zinc circuit use a mock. This can be useful for quick tests on other parts of the code surrounding the circuit. Of course, always run unmocked before you submit a PR for review

> How to set a system property? `./gradlew test slowTest -DMockZKP`.

> Please note that we have configured JUnit5 so that test classes are instantiated only once. So if you want to have tests state reset per test, add a setup method and annotate it with @BeforeAll. We follow the following best practices as much as possible: https://phauer.com/2018/best-practices-unit-testing-kotlin/

## Configuring logging

To change log levels, you can make changes in the following files:

* `notary/src/test/resources/log4j2.xml`. This file determines how our main logger logs. Note that it is only affecting test builds. The file will have no effect on the final jar. If you really want, you can even override this file by specifying your own with the following system property: `log4j.configurationFile`.
* `notary/src/test/resources/logging-test.properties`. This file determines the log levels for parts of our application (mostly dependencies) that directly use java.util.logging logger. This for instancte determines how JUnit logs before our tests run.

## Prerequisites

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