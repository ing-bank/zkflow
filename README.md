[![Build Status](https://dev.azure.com/NeoZKP/zkflow/_apis/build/status/ZKFLow%20PRs%20and%20mergest%20to%20master?branchName=master)](https://dev.azure.com/NeoZKP/zkflow/_build/latest?definitionId=1&branchName=master)

ZKFlow is a software library that enables Corda CorDapps to create private transactions on Corda.

ZKFlow enables CorDapp builders to make some or all states in a transaction private. Private states will not be present in the backchain, nor will they be disclosed to the notary. Instead, they are replaced by Zero Knowledge proofs that guarantee validity of those hidden states. Direct counterparties to a transaction do have access to all its private contents, so that they know what transaction they are signing.

For details about the ZKFLow protocol, please read the [ZKFLow white paper](docs/ZKFlow_whitepaper.pdf).

To see an example of how a CorDapp can be adapted to work with ZKFlow, please have a look at the `sample-zkdapp` in this repository.

ZKFlow is currently targeting Corda 4.8 or higher, but not yet the completely redesigned Corda 5.

Features:

- Fully private transactions: all states in a transaction are hidden
- Partially private transactions: mix hidden and public states in one transaction. Separate contract verification logic for hidden and public states
- Reveal transactions: a new transaction that makes a private state publicly visible moving forward. Previous versions of the state in its backchain remain hidden.
- Hide transactions: a new transaction that makes a public state hidden moving forward. Previous versions of the state in its backchain remain public.

To learn more about ZKFlow internals, there is a [docs](docs) directory in this repo. In addition to the [ZKFLow white paper](docs/ZKFlow_whitepaper.pdf), it contains technical documentation in Markdown and PDF format. There is also plenty of documentation throughout the code itself, explaining logic or rationale. Finally, the many test cases will also explain a lot about expected behaviour.

## Testing ZKFlow with the sample CorDapp or with your own CorDapp

The ZKFlow jars are currently not deployed to one of the Maven/Gradle repositories. This means it can not be used directly by CorDapps. Instead, run the tests for ZKFlow and the `sample-zkdapp` to see it in action.

Please make sure you have satisfied all [prerequisites](#prerequisites) before you execute the following:

```bash
$ cd sample-zkdapp
$ ./gradlew test
```

If you want to test ZKFlow with your own CorDapp, please do the following:

* Copy you CorDapp to the zkflow project, similar to `sample-zkdapp`
* Add `includeBuild("..")` to your `settings.gradle`. This will give your CorDapp access to undeployed ZKFlow artifacts
* Enable the ZKFlow Gradle plugin on your CorDapp in your `build.gradle.kts`. Note that it needs no version because of `includeBuild("..")`:

     ```kotlin
     plugins {
        id("com.ing.zkflow.gradle-plugin")
     }
     ```
* To be able to use ZKFlow's contract test DSL and other convenience functions, add `testImplementation("com.ing.zkflow:test-utils:1.0-SNAPSHOT")` to your `dependencies` block in `build.gradle.kts`.
* Finally, adapt your contracts, states, commands, contract tests and flow tests to work with ZKFlow. You can inspect `sample-zkdapp` to see how you can make those adaptations. Please note that some significant changes to your state class definitions may be required.

## Running ZKFlow tests

If you want to make changes to ZKFlow itself, you need to be able to run its tests.
This is as simple as running `./gradlew test` in the ZKFlow root directlty. It will run all tests, including integration tests.
Please make sure you have satisfied all [prerequisites](#prerequisites) before running the tests.

## Prerequisites for running ZKFlow

### Java version

This project requires Java 8. To be consistent with our CI, it is advisable to use Zulu OpenJDK 8uxxx
On Mac, that is very easy to install and manage with [SDKMAN](https://sdkman.io/).

### Zinc

You need to build and install our fork of Zinc from the [Github](https://dev.azure.com/INGNeo/ING%20Neo%20-%20ZKFlow/_git/zinc).  
Please make sure you have a recent version of Rust installed before building Zinc.

```bash
$ git clone https://dev.azure.com/INGNeo/ING%20Neo%20-%20ZKFlow/_git/zinc
$ cd zinc
$ git checkout ing-fork
$ cargo b --release
```

Built binaries will be stored in `./target/release`. Move the `zargo`, `znc` and `zvm` binaries to a directory you prefer and add it to your systems PATH. `/usr/local/bin` has been known to work. Then you can delete sources.

#### Zinc on internal Azure Pipelines

If you make changes to Zinc and create a new tag, please ensure that the [copy of the binaries](./.ci/lib/zinc-linux.tar.gz) in for Azure Pipelines is updated to be that version. Please note that these binaries should be compiled on/for linux, so they can run on the Azure Pipelines agents. 

For GitHub Actions, this is not required, as that build automatically downloads and installs the correct Zinc binaries.

### Gradle

Typically, Gradle does not require any specific changes, but you might encounter the following error during the build (path can be different):

```bash
Caused by: java.io.IOException: Cannot run program "zargo" (in directory "/Users/mq23re/Developer/zk-notary/prover/circuits/create"): error=2, No such file or directory
```

To fix it, run the command below from the project directory. It will stop daemon, thus it will clear cache, which can help to resolve the issue.

```bash
./gradlew --stop
```

## Contributing to ZKFlow

If you want to make changes to ZKFlow, great! We welcome pull requests at any time. If you decide to create a PR, please keep the [contributing guidelines](CONTRIBUTING.md) in mind.