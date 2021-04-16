# Determinants of consistent fixed length of a serialized transaction

Obvious determinants of the serialized fixed size of a transaction are`@FixedLength` annotations on collections and similar.
In addition to any `@FixedLength` annotations, transaction size is influenced by the size of specific implementations selected by the user for polymorphic types in a transaction.

An explicit example is a transaction output. Outputs are generic classes with a polymorphic type parameter, `TransactionState<T: ContractState>`, where type parameter `T` is the state class defined by the developer of a CorDapp.
The structure of that user-defined state class obviously has an impact on the serialized size of the transaction. So if that changes, it is obvious that the ZKP circuit will also have to change to reflect this.

## Conditions for consistent fixed serialized size

Fixed serialized transaction size is guaranteed to be identical only when:

* All`@FixedLength` annotations are identical
* All chosen implementations of polymorphic types are identical.

If any of those are changed, this guarantee is voided and the ZKP circuit will have to be adapted to match the new sizes.

## Assumptions

To ensure consistent serialized fixed length, we make use of the [BFL serialization scheme](https://github.com/ingzkp/kotlinx-serialization-bfl).
The above condition for polymorphic implementations used to be identical, is based on the assumption that different implementations of polymorphic types have a different serialized size.

There is a possible workaround for this: for user controlled polymorphic types such as `ContractState` implementations, it is possible to create one serialized form for all possible implementations. This form would contain a combination 
of all properties of all implementations. In that scenario, the serialized size would be constant across implementations. The obvious downside of this is that the serialized size can become prohibitively large: it will be as large as all properties of all implementations combined. The benefit of this workaround is debatable: on the one hand it will ensure consistent fixed size when a chosen implementation changes for a polymorphic type, without the user having to think about this. On the other hand, it may be preferable to have such a change explicitly fail transaction proving so that it is clear that an impactful change was made.

## Examples

As discussed, the impact of user-defined types such as implementations of `ContractState` is obvious. Some examples:

* `ContractState`
* `CommandData`

It is perhaps less obvious for certain other polymorphic transaction components or their polymorphic properties, because they are less directly influenced by the CorDapp developer. Most of them are configurable at node level and could be changed without realizing the impact. Some examples:

* `SecureHash` implementation used for transaction Merkle tree nodes. This is determined by the `HashAgility` setup for the node. This hash algo determines the size of transaction id's, which are part of `StateRefs`.
* `SecureHash` implementation used for `AttachmentId`. This is hardcoded to SHA256 in Corda for now.
* `SecureHash` implementation used for network parameters hash. This is hardcoded to SHA256 in Corda for now.
* `PublicKey` implementation. This is determined by the signature scheme used by the transacting node identities, including the Notary. Used in multiple places across a transaction and its components.
* `AttachmentConstraint` implementation set on `TransactionState.constraint`
* etc. To be investigated more
