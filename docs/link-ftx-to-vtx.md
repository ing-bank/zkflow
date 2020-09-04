Question: can we convince the Notary enough that a ZKVerifierTransaction is linked to a FilteredTransaction, so that:
* we can keep using the normal transaction types for persistence of consumed stateRefs
* we can do Notary-zkp-only, where chain-participants amongst each other simply use the normal Corda txs, and only create ZKVerifierTransaction and proof for the Notary?

## On notarisation, a prover provides:

Normal Corda:

* Current tx as ftx

ZKP Notarisation:

* Current tx as vtx
* backchains of vtxs for each input and reference of the vtx
* ZKProof for current vtx
* ZKPs for all vtxs in backchains

## Notary verifies:

Normal non-validating notary:

* validate ftx integrity 
* ftx.inputs !in spent

ZKP notary:

* resolve backchains of vtxs for each input of the vtx
* validate vtx integrity: all leaves belong to the id
* verify proof for current vtx
* validate integrity of all vtxs in backchains
* verify proofs for each ZKP in the backchain

TBD:

* validate ftx matches vtx? Possible? Necessary?

## Linking ftx and vtx

Most important to link: inputs and references of head vtx and ftx.
Both are StateRefs, but they are different: they point to a previous tx id that is calculated with a different hash function.
Additionally: in a normal FilteredTransaction, outputs are invisible.

but if we never send him a plaintext we never need to prove him this link?
mvdbos  25 minutes ago
yes, but then what inputs staterefs does it mark as spent, and which tx id does it sign as verified? Only the ZKP one. Would this be ok for the parties to receive? They will then have to make the link.
mvdbos  25 minutes ago
Which they can
mvdbos  24 minutes ago
So then a later party in the chain receives the wiretxs in the backchain, calculates the ZKProverTransaction from it, which gives it the zkid. Which it can then confirm was signed by the notary.

Conclusion: it can be done, if we don't let the notary make the link, but the parties in the chain.