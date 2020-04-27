## Thoughts

* We choose not to use the entire back-chain as secret input to the proof for performance reasons. We chose to ZCash accumulator style: an accumulator is maintained of (commitments to) ids of validly created unconsumed UTXOs. Verifiers can, with the accumulator, already confirm that the inputs that are being consumed actually exist. Next, verifiers check that they have a valid filtered merkle tree containing those ids. This will also confirm the ZKid. The ZKid is then used as public input to the ZKP verification. For this to work, the id of a State in the accumulator needs to be deterministically derivable from the contents of the state, so that the prover can prove a link between the state contents it is making a statement about, and the id of that state. 
* Ideally, we would also confirm that the ZKMerkleTree matches the normal MerkleTree of the tx. If we don't the ZKNotary will just ignore the entire normal transaction. That will be ok if normal participants do that too, but until they also become ZKP, they will not be aware of the ZKP ids. UNFORTUNATELY, we won't be able to compare the visible components of the two trees in the case of inputs and reference states, because they are StateRefs and ZKStateRefs, not only that, they are calculated from differently serialised State contents. 
* About ZKStateRef (inputs/outputs): curently it is defined as the hash of the State contents. This is and needs to be deterministic. This is not unique, because obviously, states with identical contents would have the same ZKStateRef. What could be an elegant way of making identical states unique deterministically? Require that all states have a randomly generated field (Like a LinearState)?
* Due to the fact that we use ZKStateRefs as ContractState identifiers, and therefore we have no more chain resolution, it means we need a fully resolved transaction (a LedgerTransaction) to calculate our merkle root. We can't use a WireTransaction for that, because there, inputs and references are not resolved and only StateRefs. This means that from our needs/perspective, a WireTransaction is worthless: we always need a LedgerTransaction to be available. 20201006: do we? because a transaction is simply stating that we are consuming a state with certain ref. That is enough for Merkle root calculation. For contract validation we *do* need all contents. 
* Serialization of transaction components is currently Corda's AMQP-flavor. We need a simpler format, that is easier to serialize/deserialize in a ZKP context. This small detail has an unfortunate big side-effect: it means that we can't simply send a normal `FilteredTransaction` to the notary and add a ZK proof to it. This difference in serialised format will make it impossible to recalculate a ZKP merkle root based on the serialized contents of a normal `FilteredTransaction`. We would have to calculate the additional filtered merkle tree on the prover side, and send it along. Then the verifier can verify that the visible components in it are identival to the ones in the `FilteredTransaction` and can then verify the additional Merkle tree. If those verifications are succesful, the ZKP-id can be considered equal to the normal tx-id and used as public input to ZKP verification. 

## From Slack

1) About ZKStateRef (inputs/outputs): curently it is defined as the hash of the State contents. This is and needs to be deterministic. This is not unique, because obviously, states with identical contents would have the same ZKStateRef. What could be an elegant way of making identical states unique deterministically? Require that all states have a randomly generated field (Like a LinearState)? (edited) 
2) And another question to check I am not crazy: due to the fact that we use ZKStateRefs, and therefore we have no more chain resolution, it means we need a fully resolved transaction (a LedgerTransaction) to calculate our merkle root. We can't use a WireTransaction, because there, inputs and references are not resolved. This means that from our needs/perspective, a WireTransaction is worthless: we always need a LedgerTransaction to be available. This further complicates things...
3) Also, if we use a normal FilteredTransaction as a transport, in order to keep max compatibility, we will have to calculate two ids from the same transaction contents: the normal id and the ZKid. Then the verifier can verify the normal tx with the normal id and the proof with the ZKid, knowing they are linked because they are calculated from the same data. This will even work if we serialize differntly for performance reasons: then the prover would send the normal FilteredTransaction, but also an additional ZKMerkleTree, based on custom serialization and hashing. Then when verifying, the verifier can reserialize and hash the visible components and confirm that they fit in the ZKMerkleTree without changing its root. Then the verifier will know that the visible components are the same in both the normal and the ZKMerkleTree and that the ZKid is based on the same transaction as the normal id is. (edited) 
4) But now we introduce a new variable: we dont use StateRefs, but ZKStateRefs for inputs/outputs/references. The visible inputs/reference in a FilteredTransaction are just that: refs (ids). So they are not the full contents of the States. That means that we can't calculate the ZKStateRef based on them alone, so we can't calculate the ZKid. What if we send the ZKStateRefs along as extra payload for the verification request? How can we confirm that they are the same as the normal inputs and outputs? We can't. IS THIS A PROBLEM? I would say yes, especially as long as other participants are not yet aware of ZKP. It will mean the Notary is marking spent normal States for which the ZKP is not hard linked proof. What do you think? (edited) 
Just numbered the points above. Just thinking out loud here: perhaps we can solve 4) by solving 1): what if the mandatory unique field of a ZKStateRef is the StateRef? Then we create a ZKStateRef based on a normal StateRef and its contents. If we do that, and also send the ZKStateRefs along to the Notary, we have solved 4)... But wait, we can't do that with their contents visible... :face_palm: (edited) 

Unless: ZKStateRef = H(normal StateRef + H(serialized state content bytes)). Where the normal StateRef acts as a nonce. Then we add the H(serialized state content bytes) as visible extra payload to the FilteredTransaction. 

PROBLEM: the H(serialized state content bytes) is sensitive to pre-image attack, because of no nonce.
Perhaps we can use the tx privacy salt for that? IF we do that, why not just use the component hash for that leaf as the the ZKStateRef? ZKStateRef = H(privacySalt + group + groupIdx + serializedBytes + StateRef).


When building ZKMerkleTree, for inputs/outputs/reference state components:

LeafHash = ZKStateRef = H(privacySalt + group + groupIdx + serializedBytes + StateRef)

Unfortunately, to allow a link to be made between a StateRef in the normal Merkle tree and a ZKStateRef in the ZKMerkleTree, we need to split this into two parts, which forces an extra hash operation:

ZKStateRef = H(ZKStateFingerPrint + StateRef)

Where ZKStateFingerPrint = H(privacySalt + group + groupIdx + serializedBytes)

Given that ZKStateFingerPrints for all ZKStateRefs are part of the extra tx payload received by the verifier, the verifier can:

* Verify that the normal tx Merkle tree contains its visible components. For states, if they are visible, they are visible as StateRefs.
* Verify that the ZKMerkleTree contains its visible components. For states, if they are visible, they are visible as ZKStateRefs.
* Verify that each visible component in both trees is the same:
    * For states, the verifier will confirm that StateRefs in the normal tree match ZKStateRefs at the same locations in the ZKMerkleTree. This can be done by confirming that ZKStateRef_n = H(ZKFingerprint_n + StateRef_n)

On the prover side, the secret input should contain enough information to calculate the same ZKStateRefs. This means that the secret input should also contain somehow the StateRefs for all states.  This should be no problem, because we have the StateRefs for each state in a LedgerTransaction. For outputs, it is not part of the LedgerTransaction (they are not created yet), but we can calculate them because we know the tx id and their location in the list of outputs (StateRef = txId + idx)


I think this will work, but will force the extra hash operation. Can we do this smarter? Am I missing anything?


BUT WAIT. This can be simplified. If we make StateRef part of ZKStateAndRef, we can use the ZKP to prove to the verifier that the StateRefs from the normal MerkleTree are linked to the ZKMerkleTree ZKStateRefs: ZKStateAndRef(ContractState, StateRef, ZKStateRef), where ZKStateRef = H(ContractState + StateRef). These ZKStateRefs are the components in the ZKMerkleTree.
Then in the list of StateRefs from the normal Merkle tree are part of the instance to the proof. The witness will be a ProverTransaction which has ZKStateAndRefs as components. 
Then we can verify in the proof: `instance.inputs.forEachIndexed { index, stateRef -> assert(witness.tx.inputs[index].stateRef == stateRef) }


Hey! To make the ZKStateRef unique, we could indeed just add a counter for which number input/output/reference it is in the component group, as with the normal StateRef. But we would still need some kind of nonce for pre-image attack, because the counter is known.




## Version 2:

When building ZKMerkleTree, for inputs/outputs/reference state components:

`LeafHash = ZKStateRef = H(privacySalt + group + groupIdx + serializedBytes + StateRef)`

Verifier receives a FilteredTransaction (ftx), with extra payload: ZKFilteredMerkletree and ZKP:

* Verify that the ftx Merkle tree contains its visible components. States, if they are visible, they are visible as StateRefs. (Except outputs, they are currently invisible, but would be TransactionStates. This is not what we want. Making them visible creates problems.)
* Verify that the ZKFilteredMerkletree contains its visible components. For states, if they are visible, they are visible as ZKStateRefs.
* Verify that each visible component in both trees is linked (we don't actually verify that they are the same?):
    * For states, the verifier will confirm that StateRefs in the normal tree match ZKStateRefs at the same locations in the ZKMerkleTree. This can be done by provding the list of StateRefs from the normal Merkle tree as instance parameter to the proof verification. Inside the proof circuit, the following check will confirm the verifier that the states at each location in the trees is linked:  `calculated_ZKStateRef_n = H(privacySalt + group + groupIdx + witness.ContractState_n + instance.StateRef_n)`. Next: `calculated_ZKStateRef_n equals proof.merkletree.ZKStateRef_n`. This will have value if it is done after calulating `proof.merkletree` and confirming that its root matches `instance.zkid`

On the prover side, the secret input should contain enough information to calculate the same ZKStateRefs. This means that the secret input should also contain somehow the StateRefs for all states.  This should be no problem, because we have the StateRefs for each state in a LedgerTransaction. For outputs, it is not part of the LedgerTransaction (they are not created yet), but we can calculate them because we know the tx id and their location in the list of outputs (StateRef = txId + idx)
One way of adding them, is by adding the StateRef as attribute to `ZKStateAndRef: ZKStateAndRef(ContractState, StateRef, ZKStateRef)` 



### Discussion

* Assumption check 1: do we need to prove a link between the two merkle trees? I would say yes? Otherwise, we might have a proof for different contents than the ftx.merkletree? 
* With the check in the proof above, we prove that StateRefs and ZKStateRefs are linked, but we don't prove anything about their contents being equal? 
* I think this will work, but will force the extra hash operation. Can we do this smarter? Am I missing anything?
* Also, here we don't extra hash `privacySalt + group + groupIdx`, like for the merkle tree nonce? Is that strong enough to prevent a pre-image attack on a ZKStateRef, given the attacker will know the StateRef, the group and the groupIdx? If that is a problem, does `H(privacySalt + group + groupId)` solve it?

#### Unsolved problems

With standard Corda, we can't compare outputs between ftx and ZKFilteredMerkletree. This is because standard in an ftx, outputs are filtered. If we include them, they are not StateRefs, but TransactionStates. This discloses their contents. We don't want that. To change that, we would need to change the FilteredTransaction. This changes everything, because then we diverge again further from standard Corda. Check question: do we need to prove this link for outputs? The normally non-validating doesn't care about outputs at all, so probably not there. But for back-chain validation by counterparties? They currently see all full txs, including outputs. That would go away, and be replaced with an accumulator lookup for existence of valid outputs. They validate that the inputs in the ZKMerkleTree they receive exist as valid outputs in the accumulator. To them the prover will actually open the commitment so that they can confirm the contents and use it to verify contract logic. They don't care about any link with a non-zkp states.







Out of the box: if we want to keep everything as much the same as possible and don't use the accumulator approach, but we have similar trust in the notary. Would it not be enough to have as witness parameters the notary signatures on the ZKids that we are referring to? This, included with 




Proof of Knowledge:

I prove that I know the following:

* A transaction data structure:
    * whose Merkle root is identical to the hash provided in the instance. This convinces the verifier that the proof of knowledge is for the FilteredTransaction I am asking it to validate.
    * Whose inputs and reference states are the outputs of valid transactions. Valid in this context means: signed by the notary for the FilteredTransaction:
    
        Either directly as part of this proof, or separate proofs:
        
        * For each input/ref StateRef, I prove that I know the following:
            * the filtered Merkle tree of the transaction that produced that output, where the root of that Merkle tree is equal to the input.stateRef.txId.
                * This filtered merkle tree can consist of only the top-level Merkle tree for all component groups, except the outputs. The output component group subtree contains all its leaf hashes (Blake2s), and no leaf contents. (because there might be other outputs in that tx, and we don't want to show those) (The top level tree is PH only, where all component groups are only one hash). (This might be known by the verifier and therefore part of the instance, but not currently how Corda non-validating Notary stores things.).
                * The prover should know the plaintext version of this producing transaction. (Check: is this always the case? Or should I be able to consume a state that I don't know the contents of and don't know the rest of the top-level merkle tree? THe only way to know contents, is by knowing the full tx?)
            * the contents of the output for the StateRef in this Mtree, such that the hash of these contents matches with one of the known hashes in the outputs component group leaf hashes.
            * a signature by the notary for that FilteredTransaction
            
            Such that:
            
            * The hash of the content of the output 
            * a merkle tree:
                * that contains the contents of output referenced by the input/ref
                * whose root 



Such that:

  




## Confirm: we can still do custom merkle tree and custom hashing, as long as we use the same StateRefs and components for the tree. Then we can prove that the tree is about the same content as the normal tree, or at least that the visible components are really part of both trees.












