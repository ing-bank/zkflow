## Prerequisites

This project makes use of our fork of Corda maintained here: https://github.com/ingzkp/corda/tree/ing-fork
This fork is based on the latest version of Corda and will have all our proposed PRs to Corda already merged.
The artifacts for our fork are deployed to Github Packages and this project is aware of that and will be able to find Corda dependencies there

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

