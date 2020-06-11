# Discussion document: changes to Corda enabling custom transaction verification

The context for this document is the ING's project to add zero-knowledge transaction verification to Corda. Both for Notaries and for other transaction participants.
The requirements for enabling zero-knowledge transaction verification can be generalised to requirements that allow any form of custom transaction verification.

Below, we describe the issues we have encountered so far in integrating custom transaction verification into Corda and where possible we suggest solutions. These are of course subject to further discussion between the R3 and ING teams. The summary is that implementing any custom transaction verification currently requires re-implementing a considerable chunck of the core Corda protocol, because most of Corda was not implemented to support extension of its protocol at this fundamental level. That said, we believe that Corda's design supports it quite well, it is mostly a matter of changing implementation, not design.

We see two types of changes to Corda: 

- Changes that are in our opinion required regardless of this project to ensure the future-proofness of the Corda platform.
- Changes for which that is less obvious and that are more closely tied to custom transaction verification.

> A note on intellectual property: without being explicit about the solution direction, it will be very hard to reason together about the issues encountered trying to implement it. Given what was already openly discussed at CordaCon 2019 and on the Corda Development mailinglist in the past, and given the public information in our pending patent application, we can safely state that we are looking at a solution where we add ZKP-related information to existing transaction data, while leaving the existing transaction verification logic unchanged where possible. This may involve an additional Merkle tree, based on ZKP-friendly hash algorithms.  

> A note on scope: this document is an initial list, based on our experience so far with implementing Zero Knowledge notarisation only. We expect to encounter new issues in the next few iterations of our design and when we start to include back-chain privacy. We would like to be able to discuss those as they arise.

## Changes that ensure future-proofness of the Corda platform

We expect that these are all addition-only changes, and should not have any impact on current APIs.

* We would like to add support to `SecureHash` for ZKP-friendly hash algorithms. Corda uses `SecureHash` everywhere. Currently, this is hardcoded to be SHA-256. We expect that future use cases of Corda will require different hash algorithms that are more suitable to that context. ZKP is one, but light mobile clients and IOT-devices also come to mind. Another consideration is quantum resistance.  We have some thoughts on how this could be done. See also this discussion on the Corda Dev mailinglist: https://groups.io/g/corda-dev/topic/adding_support_for_different/71512259.
* Add support for ZKP-friendly signature schemes. Similar considerations apply to the signature schemes that are currently supported. This is a limited, hardcoded set.  
* Perhaps more controversial: we would like to open up certain core Flow classes for extension. As for the hash algorithms, we expect future use cases of Corda to require lighter/differnt variants of core flows such as the `ResolveTransactionsFLow`, `FinalityFlow`, `CollectSignaturesFlow` and both client and service side of NotarisationFlows. See below for details.

### Open up core Flow classes for extension

A few core flows need to be opened for extension. Currently, they are not properly designed for that: they are either simply final, make a lot of assumptions in private functions, or the extension points are insufficient. This means we need to copy/paste them. oWe want to reuse as much core logic as possible, but we need to be able to deviate where needed. 

Some examples  (not exhaustive):

* The interaction between transaction participants to come to a `SignedTransaction` will have to be adapted to prevent extra roundtrips. First, the participant will (also) sign the ZKP Merkle root (in addition to the normal one). They will do that with a ZKP-friendly signature scheme. This means we will have to change (or replace) the `CollectSignaturesFlow`. 
* We will introduce a custom Notary Client Flow, that is aware of ZKP and will send the correct payload to the notary. `NotaryFlow.Client`'s support for extension is not complete and requires a lot of copy-paste.
* It is not configurable which NotaryFlow the `FinalityFlow` starts. This means copying the entire flow and changing only a few lines.
* On the Notary service side, `NotaryService` is properly extendable, but this is not the case for `NotaryServiceFlow` and `NonValidatingNotaryFlow`. Both have private logic that makes too many assumptions about the contents of a transaction.

## Changes that enable custom transaction verification

### Allow additional transaction components or more flexible notarisation payload

Assuming we will use a non-validating Notary as a basis, we will need to send along some additional data with the `FilteredTransaction` in the `NotarisationPayload`. There are multiple ways to achieve this:

1. Add additional transaction components to support arbitrary payload. The custom notary can then check for these components and handle them if present.
2. Introduce a custom transaction type that is *not* a `CoreTransaction`, but can be a `BaseTransaction`. This document is not the right place to explain the details of why that is and how, but one of the consequences of this is that the `NotarisationPayload` is not flexible enough, as it supports only `SignedTransaction` and `CoreTransaction`.

In both cases, changes will be needed to support more flexibility in what we can send to the notary.

### Make certain APIs more flexible 

The `UniquenessProvider` assumes a lot on what parts of a transaction are required to determine uniqueness. It would be more flexible to take the full tx as input for commit and let the implementation decide how to use it to determine uniqueness. As it is now, we will have to replace it completely.

### Other changes

* Setting CorDapp config in tests: this is possible for tests based on `MockNetworParameters` (with `TestCordappImpl.withConfig()`), but is impossible for simpler tests, which make use of MockServices.
* `JacksonSupport` Mixins are all private, we want to reuse them and/or be able to extend `JacksonSupport` to support more custom types.

As noted above, this document is an initial list, based on our experience so far with implementing Zero Knowledge notarisation only. We expect to encounter new issues in the next few iterations of our design and when we start to include back-chain privacy. We would like to be able to discuss those as they arise. 







