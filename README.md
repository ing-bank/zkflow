The ZKFlow Protocol
===================

The ZKFlow consensus protocol enables private transactions on Corda for arbitrary smart contracts using Zero Knowledge Proofs.

ZKFlow enables CorDapp builders to make some or all states in a transaction private.
Private states will not be present in the backchain, nor will they be disclosed to the notary. 
Instead, private states are replaced a Zero Knowledge proof that guarantees validity of those hidden states according to the smart contract.

Direct participants to a transaction will always have access to all its private contents, so that they know what transaction they are signing.
Future owners of a state will never see the private contents of previous transactions, unless another participant actively discloses them.

For details about the ZKFlow protocol, please read the [ZKFlow white paper](docs/ZKFlow_whitepaper.pdf).

## Features
- Fully private transactions: all states in a transaction are hidden
- Partially private transactions: mix hidden and public states in one transaction. Separate contract verification logic for hidden and public states
- Reveal transactions: a new transaction that makes a private state publicly visible moving forward. Previous versions of the state in its backchain remain hidden.
- Hide transactions: a new transaction that makes a public state hidden moving forward. Previous versions of the state in its backchain remain public.
- a ZKP-aware notary
- Seamless usage: 
  - ZKP-aware smart contract test DSL
  - drop-in ZK replacements for core Corda consensus flows
- ZKFlow currently targets Corda 4.8+, not the upcoming Corda 5

To see an example of how a CorDapp can be adapted to work with ZKFlow, please have a look at the [sample ZKDapp](./sample-zkdapp) in this repository. A good starting point is [ExampleToken.kt](./sample-zkdapp/src/main/kotlin/com/example/contract/token/ExampleToken.kt).

## Performance
By nature, the setup phase of ZKP circuits and creating proofs is not a fast operation. This means that ZKFlow is perhaps not best suited to use cases where a transactions are created with a very high frequency. That said, proving time has been show to be reasonable for many use cases.

On a 2.6Ghz 6-core Intel i7, proving takes ~0.012 seconds per byte of witness size (the secret input). This scales linearly.
Memory usage also scales linearly with witness size and is roughly equal to the size of the proving key for that command's circuit:

For the sample ZKDapp, that amounts to the following, indicative, numbers: 
```
------------------------------------------------------------
| Command       | Witness size | Proving mem | Proving time |
------------------------------------------------------------
| IssuePrivate  | 1344 bytes   | ~250MB      | ~23s         |
| MovePrivate   | 3185 bytes   | ~500MB      | ~38s         |
| SplitPrivate  | 4430 bytes   | ~800MB      | ~53s         |
| RedeemPrivate | 1951 bytes   | ~250MB      | ~23s         |
------------------------------------------------------------
```

### What affects performance most?
The majority of computation time is not due to smart contract logic, but mostly due to the size of the witness and the deserialization of the transaction components inside it.

The witness contains all transaction components that are private, or that are required to do the verification.
Unfortunately, even if your state class is tiny, and you do only an issuance (i.e. only one state in the transaction), the witness
is still fairly big. This is because for a transaction to be considered valid in Corda, there are more rules than only the smart contract rules that need to be verified. Some of those platform rules require additional transaction components in addition to your state classes.
One example is the notary transaction component. This will always be in the witness and its size alone is ~500 bytes, which is significant. This is because a notary is hardcoded to be a `Party` in Corda, and this contains a `CordaX500Name`. To reliably make that class fixed length without creating runtime isssues, ZKFlow sets `CordaX500Name`'s fixed field sizes to those specified in that class.


# Getting Started

> [!] **DISCLAIMER: Please note that ZKFlow in its current state should *NOT* be used in production, nor to transfer ownership of real assets.**
>
>Even though the protocol has been peer reviewed, the security of the implementation has not yet been reviewed by a third party. Additionally, There are features related to deployment and ZKP artifact distribution that are not present in this repository but that are important to guarantee secure and correct usage.

## Running the sample ZKDapp

The sample ZKDapp demonstrates how a basic token contract can be adapted to work with ZKFlow. The contract tests and flow tests show the expected behaviour, i.e they behave as you would expect from normal Corda.
Please see [ExampleToken.kt](./sample-zkdapp/src/main/kotlin/com/example/contract/token/ExampleToken.kt) for details on how to adapt a state class. It is documented extensively.

Please make sure you have satisfied all [prerequisites](#Prerequisites for running ZKFlow) before you execute the following:

```bash
$ cd zkflow/sample-zkdapp
$ ./gradlew test
```

Please note that running these tests can take a long time. Contract tests are slowish because they simulate ZKP proving and verifying using the real ZKP circuit, but especially the flow tests are slow: they use the real ZKP circuit and will do the trusted setup every time changes are made to the private smart contract. This can take minutes. Of course, in a non-test situation, this setup would be done only once. 

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
 
Now you are ready to adapt your contracts, states, commands, contract tests and flow tests to work with ZKFlow. 
Please see [ExampleToken.kt](./sample-zkdapp/src/main/kotlin/com/example/contract/token/ExampleToken.kt) for guidance on how to adapt a state class. It is documented extensively.

## Troubleshooting the sample ZKDapp or your own ZKDapp

See [Troubleshooting](./docs/troubleshooting.md).

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

## Reporting issues

If you found a bug or security issue, feel free to open an issue her on GitHub. 

## License

[MIT](./LICENSE)
