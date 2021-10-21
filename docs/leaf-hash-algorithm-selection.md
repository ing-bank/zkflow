# Leaf hashing algorithm considerations

In order to achieve good performance of proving it is important to use hashing function that is optimized to be executed inside circuit. For now main candidate for that is Pedersen hash that we already use for Merkle tree nodes hashing.

From the other  hand - hashing of large inputs (e.g. outputs in case of Corda tx) can be very slow on Corda side as Pedersen hash is significantly slower in normal execution context than hashes like SHA or Blake.

So the problem is that we need to either sacrifice proving time or verification time. Here by verification we mean a complete tx verification in Corda context, not just verification of a tx.

Proving is the slowest part of the system and any gain here will significantly improve any end-2-end tx creation scenario, but from the other hand verification of a transaction happens not only during tx creation phase but also for the whole tx lifetime, including backchain verification. Also, it happens a lot on a Notary which is sort of a bottleneck in Corda. However, during verification party only needs to calculate hash of 'open' components group, which are: 
* Inputs
* References
* Notary 
* Timewindow
* Network Parameters
* Signers
* Commands

Most of these are relatively small in most cases and should not exceed 1 KB, that takes around 100ms to hash with Pedersen.

### Proving efficiency

In TransactionVerificationTest numbers for proving only for Create and Move Txs respectively:
Blake - 11s/16s
PH - 9s/11s

Amount of speed increase can vary depending on size of components and roughly estimated to be in 20-50% range.

### Hash speed during verification

Using exponentiation tables - 100ms per 1 KB on average, grows linearly with input size.

Also requires time to build exp table - around 1.5s per 1 KB, but this is only required once (e.g. during node startup)

## Conclusion

The conclusion is that Pedersen hash does not provide enough impact on proving side to justify its slow work at verification side.

The decision is to try new modern ZKP-friendly hash functions, namely Rescue, Poseidon, etc. They are expected to perform better on both proving and verification sides. From security perspective they may look even less 'standard crypto' but their adoption is rising so they become more and more battle-tested over time.