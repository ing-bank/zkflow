# Multiple commands in a transaction - Design

## Scenarios

There are a few variants when it comes to supporting multiple commands per transaction.

1. All commands are `ZKCommandData`. This means that all tx contents related to those commands should be part of the ZKP smart contract checks.
2. Not all commands are `ZKCommandData`. This is split in two cases:
    1. Only the tx contents related to the ZKP commands should be private and therefore part of the ZKP smart contract checks. Other contents will be visible, and normally validatable.
    2. All tx contents should be part of the ZKP and hidden in the normal tx. This situation is identical to variant '1', except that for some reason it is not possible to make all commands `ZKCommandData`. This would be the case when using commands from third party CorDapps.

> Note:
> ZKFlow expects that for any ZKP transaction the first command in its command componentgroup is a `ZKTransactionMetadataCommandData` that describes the structure of the entire transaction, including any other commands it contains. Each command included in a ZKFLow transaction should have `ZKCommandMetadata` defined, either directly or through an extension property.

> Note:
> Regardless of whether a command and its related transaction components are private, they are always included in the witness as serialized component groups. This is because we still need them to calculate the Merkle root (transaction id) of the transaction, even if the circuit is otherwise not interested in them.

To support the above variants, the user should specify for each command in a transaction whether it is private or not. This is specified in the circuit metadata of that transaction (i.e. in the circuit metadata of the command that is expected to be placed as the first command in that transaction).

* If a command is private, ZKFlow will expect to find a contract rule definition and related states for it under `src/main/zinc` of the user's CorDapp or possibly in a dependency library that provides the smart contract. These will be combined into one ZKP circuit that covers all private commands and the tx components related to them. These components will be hidden in the `ZKVerifierTransaction`.
* If a command is not private, the ZKP circuit smart contract checks will not cover it and the tx components related to it will be visible in the `ZKVerifierTransaction`. Validators will validate those contracts and components normally.

## Circuit metadata

Like other structural information about transactions, commands are described in the circuit metadata. To ensure that our software can relate commands to their transaction components, the circuit metadata DSL is grouped by command, each specifying their components. When this information is flattened, so that we can create e.g. the outputs component group, it is assumed that the outputs are ordered in the order the commands appeared in the circuit metadata.

### Example

```kotlin
// DepositContract.Advance
object Advance : DepositContractCommand, ZKTransactionMetaData {
    // This command will be the first command for the transaction and therefore will be a `ZKTransactionMetaData` which defines this property:
    val transactionMetaData = transactionMetadata {
        network { // These should be consistent across all commands in a transaction. Therefore they are applied at transaction level here.
            attachmentConstraintType = SignatureAttachmentConstraint::class
            publicKeyType = EdDSAPublicKey::class
        }

       /**
        * Through this parameter, the user tells ZKFlow how many ContractAttachments are expected to be attached to this transaction.
        * ZKFLow uses this to verify the number of attachments in a transaction, but most importantly to generate Zinc code.
        * 
        * A CorDapp is a essentially a jar that provides implementations for contract/command code
        * These jars are attached to transactions that use code contained in them.
        * When a tx is verified, a classloader is created from these jars. This way, the types (commands, states, etc.) mentioned in the 
        * contract verify function will be available on the classpath. 
        * 
        * Unfortunately, the number of CorDapps required to supply all contract/command code can not be determined at compile time:
        * CorDapp jars are loaded from the filesystem at Corda node startup. Information about them (like which contract classes they support), is stored in the ServiceHub of the node. This is only available at runtime.
        * 
        * TODO: We could call `ResolvedZKTransactionMetadata.verify(txb: ZKTransactionBuilder)` with a second `ServiceHub` parameter from `ZKTransactionBuilder.toWireTransaction()`, but we can't do this from the verify function which is called from the smart contract. For now, we use this hardcoded parameter. If it is not provided, it will default to the number of unique package names for the different ContractClasses found in the transaction (which is naive).
        */
       numberOfCorDappsForContracts = 3
       
        /**
         * These are the commands in this transaction.
         *
         * Commands can be:
         * 1. User command AND ZKCommandData
         * 2. User command AND NOT ZKCommandData
         * 3. Third party command AND ZKCommandData
         * 4. Third party command AND NOT ZKCommandData
         *
         * When a command is included in a transaction metadata as below, they are expected to have `commandMetadata` property defined that specifies the transaction structure for that command. ZKFLow will look for the metadata in the following locations:
         * - The command is a ZKCommandData: the interface enforces that it will have a `commandMetadata` property.
         * - The command is NOT a ZKCommandData: ZKFlow will look for a `commandMetadata property defined as an extension property on that command. It will look for it the companion objects of all ZKCommands that are also part of this transaction. Most likely place to put this would be the companion object of the command that has the ZKTransactionMetaData
         */
        commands {
            command(DepositContract.Advance::class) // A user command AND a ZKCommand, it should have `commandMetadata` defined.
            command(TokenContract.Command.Issue::class) // A third party command AND NOT a ZKCommand. User will have to define `commandMetadata` extension property
            command(ZKFLow.Command.SomeCommand::class) // A third party command AND a ZKCommand. The third party will have defined the commandMetadata on it, including where the circuit files can be found if it is private.

            // In the future it may also be possible to override a command's `commandMetadata` here, like this:
            // command(ZKFLow.Command.SomeCommand::class) { // A third party command AND a ZKCommand. The third party will have defined the commandMetadata on it, including where the circuit files can be found if it is private.
            //     circuit { name = "zkflow-some" } // Or autogenerated from command class name
            //     numberOfSigners = 2
            //     private = true // This determines whether a circuit is expected to exist for this command. If false, ZKFLow will ignore this command for the ZKP circuit in all ways, except for Merkle tree calculation.
            //     inputs {
            //         1 of DepositState::class
            //     }
            //     outputs {
            //         2 of SomeOtherDepositContractState::class
            //     }
            // }
        }
    }

    /** This is the commandMetadata for DepositContract.Advance command. The commandMetadata for the other commands in `transactionMetaData` above will define their own commandMetadata property
     */
    val commandMetadata = commandMetadata {
        circuit { name = "deposit-advance" } // Or autogenerated from command class name
        numberOfSigners = 2
        private = true // This determines whether a circuit is expected to exist for this command. If false, ZKFLow will ignore this command for the ZKP circuit in all ways, except for Merkle tree calculation.
        inputs {
           1 of DepositState::class
        }
        outputs {
            1 of DepositState::class to 1
            2 of SomeOtherDepositContractState::class
        }
    }
}

/**
 * The user should define this extension property on the companion object of one of the ZKCommands that are in the same transaction. Most likely place is the the command that has the ZKTransactionMetaData
 */
val TokenContract.Command.Issue.commandMetadata: ZKCommandMetadata
    get() = commandMetadata {
        /**
         * This command (and therefore its associated tx components) is not private. This means no Zinc contract rules need exist for it: it will be ignore by the Zinc circuit for everything, except Merkle tree calculation. For the Merkle tree calculation and fixed witness size it is still necessary to define the size of all components for this command, same as for private commands.
         */
        private = false
        numberOfSigners = 1
        outputs {
            1 of TokenState::class
        }
    }
```

In this example, after processing the `transactionMetaData` and the `commandMetadata` for each command, the outputs group of the transaction would look like this:

```kotlin
outputs[ // The order of these outputs depends on the order the commands are defined on the `transactionMetaData`.
    DepositState,
    SomeOtherDepositContractState,
    SomeOtherDepositContractState,
    TokenState
]
```

## TransactionBuilder

The following invariants will be enforced:

* The first command is a `ZKCommandData`. Its `circuitMetadata` will determine the structure for the entire transaction, so it will be aware of the impact any other commands may have on transaction structure and contents. The circuit metadata of the user's custom command should describe the entire transaction structure, including the contents from any third party library. E.g. a CorDapp builder may have a transaction that transitions their own states, governed by their own commands. This transaction can also simultaneously transition third party states governed by a command from a third-party contract, such as the Tokens SDK contracts.
* The number of signers is the upper bound on the number of signers possible. Less is possible. In that case, the signers list for this command will be padded to the required number of signers with empty Public Keys, so that the fixed size of the transaction does not change.