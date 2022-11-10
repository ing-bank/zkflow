The ZKFlow consensus protocol enables private transactions on Corda using Zero Knowledge Proofs.

ZKFlow enables CorDapp builders to make some or all states in a transaction private. 
Private states will not be present in the backchain, nor will they be disclosed to the notary. 

Instead, private states are replaced by Zero Knowledge proofs that guarantee validity of those hidden states according to the smart contract.
Direct participants to a transaction will always have access to all its private contents, so that they know what transaction they are signing.
Future owners of a state will never see the private contents of previous transactions, unless another participant actively discloses them.

For details about the ZKFlow protocol, please read the [ZKFlow white paper](docs/ZKFlow_whitepaper.pdf).

To see an example of how a CorDapp can be adapted to work with ZKFlow, please have a look at the [sample ZKDapp](./sample-zkdapp) in this repository.

Features:
- Fully private transactions: all states in a transaction are hidden
- Partially private transactions: mix hidden and public states in one transaction. Separate contract verification logic for hidden and public states
- Reveal transactions: a new transaction that makes a private state publicly visible moving forward. Previous versions of the state in its backchain remain hidden.
- Hide transactions: a new transaction that makes a public state hidden moving forward. Previous versions of the state in its backchain remain public.
- drop-in ZK replacements for core Corda consensus flows
- a ZKP-aware notary

To learn more about ZKFlow internals, there is a [docs](docs) directory in this repo. In addition to the [ZKFlow white paper](docs/ZKFlow_whitepaper.pdf), it contains technical documentation in Markdown and PDF format. There is also plenty of documentation throughout the code itself, explaining logic or rationale. Finally, the many test cases will also explain a lot about expected behaviour.

ZKFlow is currently targeting Corda 4.8 or higher, it does not support the upcoming Corda 5.

## Running the sample ZKDapp

The sample ZKDapp demonstrates how a basic token contract can be adapted to work with ZKFlow. The contract tests and flow tests show the expected behaviour.

Please make sure you have satisfied all [prerequisites](#Prerequisites for running ZKFlow) before you execute the following:

```bash
$ cd zkflow/sample-zkdapp
$ ./gradlew test
```

## Running your own CorDapp with ZKFlow

> [i] This assumes you use the Kotlin DSL for your Gradle build files. If you do not, change accordingly for the Groovy DSL. The changes should be identical except for syntax.

The ZKFlow jars are currently not deployed to one of the public Maven/Gradle repositories. If you want to test ZKFlow with your own CorDapp, please make the following changes to your build file:

* Make sure that ZKFlow is published to your local Maven repository by running `./gradlew publishToMavenLocal` in the ZKFlow directory. 
* Add the `mavenLocal()` repository to your repositories for Gradle plugins (probably in the `pluginManagement` block in your `settings.gradle.kts`) and to your repositories for normal Gradle dependencies in your `build.gradle.kts`. 
* Enable the ZKFlow Gradle plugin on your CorDapp in your `build.gradle.kts`.

     ```kotlin
     plugins {
        id("com.ing.zkflow.gradle-plugin") version "1.0-SNAPSHOT"
     }
     ```
* To be able to use ZKFlow's contract test DSL and other convenience functions, add `testImplementation("com.ing.zkflow:test-utils:1.0-SNAPSHOT")` to your `dependencies` block in `build.gradle.kts`.
 
Not you are ready to adapt your contracts, states, commands, contract tests and flow tests to work with ZKFlow. You can inspect `sample-zkdapp` to see how you can make those adaptations. Please note that some significant changes to your state class definitions may be required.

Not recommended, but feel free to run it immediately as-is and let ZKFlow tell you what should be added to your classes.

## Troubleshooting the sample ZKDapp or your own ZKDapp

Build issues:
- `java.lang.IllegalArgumentException: Cannot find circuit manifest`: This should not happen during normal development flow, but it can be solved by running `./gradlew generateZincCircuits --rerun-tasks` in your ZKDapp. 

## Running ZKFlow tests

If you want to make changes to ZKFlow itself, you need to be able to run its tests.
This is as simple as running `./gradlew test` in the ZKFlow root directly. It will run all tests, including integration tests.
Please make sure you have satisfied all [prerequisites](#Prerequisites for running ZKFlow) before running the tests.

## Prerequisites for running ZKFlow

### Java version

This project requires Java 8. To be consistent with our CI, it is advisable to use Zulu OpenJDK 8uxxx
On Mac, that is very easy to install and manage with [SDKMAN](https://sdkman.io/).

### Zinc
Zinc is the ZKP toolchain used under the hood by ZKFlow. It is created by [Matter Labs](https://matter-labs.io/).
Zinc was forked for ZKFlow to enable a few new features.

You need to build and install the ZKFlow fork of Zinc from [Github](https://github.com/mvdbos/zinc).  
Please make sure you have a recent version of Rust installed before building Zinc.

```bash
$ git clone https://github.com/mvdbos/zinc
$ cd zinc
$ git checkout ing-fork
$ cargo b --release
```

Built binaries will be stored in `./target/release`. Move the `zargo`, `znc` and `zvm` binaries to a directory you prefer and add it to your systems PATH. `/usr/local/bin` has been known to work. Then you can delete sources.

## Contributing to ZKFlow

If you want to make changes to ZKFlow, great! We welcome pull requests at any time. If you decide to create a PR, please keep the [contributing guidelines](CONTRIBUTING.md) in mind.

## License

[MIT](./LICENSE)
