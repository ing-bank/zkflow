## Prerequisites

This project makes use of our fork of Corda. You will need to install that version to your local Maven repo like so:
```bash
$ git clone https://github.com/ingzkp/corda
$ cd corda
$ git checkout ing-fork
$ ./gradlew install installDist
```

> The branch `ing-fork` contains a version of corda that is based on the latest version of Corda and where all our proposed PRs to Corda are already merged.

## Tests to use:

`com.ing.zknotary.notary.transactions.VictorsSerializeProveVerifyTest`

This test will give you: 

* a proper transaction data structure
* a place to test serialization logic 
* an opportunity to test proving and verifying e2e between Kotlin and Zinc.

Feel free to created dedicated unit tests for the serializer.

## Serializer to implement:

`com.ing.zknotary.common.serializer.VictorsZKInputSerializer`

If you have a better name once you know how you will do it, please feel free to rename it. :-)

I (Matthijs) will also implement a naive JSON/CordaSerialized serializer for inspiration/reference: `com.ing.zknotary.common.serializer.JsonZKInputSerializer`.
If we can deserialize CordaSerialized components in Zinc into meaningful structures, this might even work.

## Prover/Verifier to implement:

Prover: `com.ing.zknotary.common.zkp.ZincProverCLI`

Verifier: `com.ing.zknotary.common.zkp.ZincVerifierCLI`

We have agreed to initially do it the CLI way, so that we can focus on the serialization/deserialization logic first.
Once we have that in place, we will move to `ZincProverNative` and `ZincVerifierNative`.

> Please note that the ZKId of a transaction (our custom Merkle root) is currently calculated based on the 
> CordaSerialized form of transaction components. We may be able to change that to another format, but if not, we will have to
> pass that format to Zinc as well to recalculate the ZKId. Then we will have to deserialize it to verify the validity of the contents.

