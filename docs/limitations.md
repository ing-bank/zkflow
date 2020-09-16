# Limitations compared to normal Corda

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

# Fixed lengt

* Until Zinc starts to support arbitrary length loops, the following needs to be fixed length (hardcoded in the circuit):
    * number of tx components in all groups
    * the size of each component, and of each attribute of each component