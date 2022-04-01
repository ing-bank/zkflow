# Limitations compared to normal Corda

## Narrower types where possible

* Don't use `Party` or `AbstractParty` in your states and commands. Use AnonymousParty or some other form of AbstractParty that does not store the CordaX500Name. This is because the CordaX500Name adds up to a lot of bytes, but is not strictly necessary. If you really need to know the CordaX500Name, you can always look it up in your flows. This does mean you will not have access to the CordaX500Name in your contracts.
* ZonedDateTime: make sure `ZoneId` is a `ZoneOffset` and not a `ZoneRegion`. The former is an Int (offset in seconds), so 4 bytes. The latter is a string of 32 characters (64 bytes in UTF-8) to fit the longest region name. This saves 60 bytes.
* TBD: allow only ASCII strings: this saves a byte for every character. This can add up *a lot*.
  * Strings can be encoded as ASCII, UTF-8, UTF-16 or UTF-32 byte arrays, the corresponding annotation limits the number of bytes, not the number of caracters/code points.
* Surrogates for polymorphic types:
    * Use a different surrogate for each implementation. Don't use one surrogate for all types. This requires it to fit the largest one, meaning that it will always be that size.
    * Use a custom, short `@SerialName` for each surrogate, that is as short as possible, but identifies it uniquely across the system. (TODO: create an annotation scanner for it that checks uniqueness of `@SerialNames`)

## Transaction size

* Limited number of inputs/outputs/references (4 each?)

## Transactions structure

* Transactions can have only one command for performance reasons. This means that a use case that for example handles cash for assets, would have to have a contract that has states defined for both cash and assets. We may start to support multiple commands/contracts per tx later, which means splitting them out to separate txs and therefore create separate proofs. Intially, a proof circuit will be for contract/command combination, perhaps performance will allow for it to be per contract later.

## State and CommandData classes

* For now only support TypeOnlyCommandData for commands
* Encumbering states is not supported, this should be solvable with reference states or simply with contract rules
* We do not support standard provided contracts (yet): Cash, Token SDK, etc
* Floating point types are not supported. Please use BigInteger or similar.

## General

* Will require additional knowledge on part of developer. Our goal is to minimize as far as possible

# Differences (not necessarily limitations)

* We leak the contract and command information to the verifier, because we use one circuit per contract command for performance reasons, and because the verifier must know which circuit (verifier key) to use.
* All states in a transaction must belong to one contract and circuit
* No support for attachments within the circuit, too heavy to have as part of witness
* Smart contracts and linked cicruit are not in attachments but previously distributed. Tx contains a reference to that contract version
* Output leaf hashes are visible in a FilteredTransaction, we need them to compare against when validating ZKPs for txs using those outputs to prove that we did not change the contents. This leaks the number of outputs in a tx. This should not be an issue, since we already leak the command?

To be discussed: how to manage that for each command, there can be a mix of state types within inputs/outputs/references. E.g. I sell a house. Inputs contain state of type HouseState and of type CashState, so do outputs. Perhaps there is also a SaleAgreementState as an additional output, and a reference state of type GovernmentHouseIdentifierState

# Fixed length

* Until Zinc starts to support arbitrary length loops, the following needs to be fixed length (hardcoded in the circuit):
    * number of tx components in all groups
    * the size of each component, and of each attribute of each component

# State/Command contents

* Our custom leaf hash serialization does not support nullable types in collections. So List<Int?> is not supported for use in your custom Cordapp classes such as states and commands that are part of the tx Merkle tree. This could change when we move to two-serialization for this (if necessary), because then we would add metadata to each the serialized form of each item that prevents the attack where without metadata, but supported nulls, an attacker could craft a list that is equal to a list containing nulls by carefully choosing the non-null values to serialize as a null value.
