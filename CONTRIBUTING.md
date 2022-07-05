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

## Configuring logging during testing

To change log levels, you can make changes in the following files:

* `config/test/log4j2.xml`. This file determines how our main logger logs. Note that it is only affecting test builds. The file will have no effect on the final jar. If you really want, you can even override this file by specifying your own with the following system property: `log4j.configurationFile`.
* `config/test/logging-test.properties`. This file determines the log levels for parts of our application (mostly dependencies) that directly use java.util.logging logger. This for instance determines how JUnit logs before our tests run.

## Verifying test coverage

To assess the test coverage, you can run `./gradlew test jacocoRootReport`. The coverage reports can be found in `build//reports/jacoco/aggregate/`.

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

