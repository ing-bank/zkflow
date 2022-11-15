package com.example.contract.token.commands

import com.example.contract.token.ExampleToken
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import org.intellij.lang.annotations.Language

@ZKP
class MovePrivate : ZKCommandData {
    /**
     * To ensure an entire transaction can be made fixed length
     * by ZKFlow, we not only need to annotate types with size
     * information, we also need to tell ZKFlow what the structure
     * of a transaction is for a specific command. This is different
     * from standard Corda, where smart contracts can handle a dynamic
     * number of inputs and outputs to apply the contract rules to.
     *
     * The best way to specify this metadata is by using the
     * metadata DSL like below.
     *
     * ```
     * override val metadata = commandMetadata {
     *     numberOfSigners = 1
     *
     *     notary = true
     *
     *     // Specifies the number and type of references in this transaction.
     *     // It also specifies whether they are private or allowed to be public.
     *     // Finally it specifies the index at which this reference is in the
     *     // transaction's list of references. Note that ZKFlow will complain
     *     // when there are multiple commands in a transaction that expect
     *     // references of different type at the same index.
     *     //
     *     // Note that for references, 'private' means that ZKFlow enforces
     *     // that the referred UTXO is private on the ledger, i.e. that it was
     *     // a private output in its transaction. This is to prevent inadvertent
     *     // hiding of public components and ensures that transactions that
     *     // are intended to be fully private actually are.
     *     //
     *     // See docs block on [ZKReferenceList] for more details.
     *     references {
     *         private(ExampleToken::class) at 0
     *     }
     *
     *     inputs {
     *         private(ExampleToken::class) at 0
     *     }
     *
     *     // Specifies the number and type of outputs in this transaction.
     *     // It also specifies whether they are created private or public.
     *     // Finally it specifies the index at which this outputs is in the
     *     // transaction's list of outputs. Note that ZKFlow will complain
     *     // when there are multiple commands in a transaction that expect
     *     // outputs of different type at the same index.
     *     //
     *     // Note that for outputs, 'private' means that ZKFlow enforces
     *     // that the UTXO is a private output in its transaction.
     *     // This is to prevent inadvertent revealing of private components and
     *     // ensures that transactions that are intended to be fully private actually are.
     *     //
     *     // See docs block on [ZKProtectedComponentList] for more details.
     *     outputs {
     *         private(ExampleToken::class) at 0
     *     }
     *
     *     // Set to true of the private smart contract requires a timeWindow
     *     timeWindow = true
     * }
     * ```
     */
    override val metadata = commandMetadata {
        // Specifies the number and type of inputs in this transaction.
        // It also specifies whether they are private or allowed to be public.
        // Finally, it specifies the index at which this input is in the
        // transaction's list of inputs.
        //
        // Note that ZKFlow will complain
        // when there are multiple commands in a transaction that expect
        // inputs of different type at the same index.
        //
        // Note that for inputs, 'private' means that ZKFlow enforces
        // that the referred UTXO is private on the ledger, i.e. that it was
        // a private output in its transaction. This is to prevent inadvertent
        // hiding of public components and ensures that transactions that
        // are intended to be fully private actually are.
        //
        // This command contains no references, but declaring references
        // is identical to declaring inputs.
        //
        // See docs on [ZKCommandData] and [ZKReferenceList] for more details.
        inputs {
            private(ExampleToken::class) at 0
        }

        // Specifies the number and type of outputs in this transaction.
        // It also specifies whether they are created private or public.
        // Finally, it specifies the index at which this outputs is in the
        // transaction's list of outputs.
        //
        // Note that ZKFlow will complain
        // when there are multiple commands in a transaction that expect
        // outputs of different type at the same index.
        //
        // Note that for outputs, 'private' means that ZKFlow enforces
        // that the UTXO is a private output in its transaction.
        // This is to prevent inadvertent revealing of private components and
        // ensures that transactions that are intended to be fully private actually are.
        //
        // See docs on [ZKCommandData] and [ZKProtectedComponentList] for more details.
        outputs {
            private(ExampleToken::class) at 0
        }

        // Set to true of the private smart contract requires a timeWindow
        timeWindow = true

        // Specifies whether the notary components is required for the
        // smart contract, i.e. if the transaction is notarised.
        notary = true

        // This tells ZKFlow how many signers to expect for this command.
        // It is an error if there are less or more signers.
        //
        // Combined with a contract rule that checks that the right key
        // has signed, this behaviour approximates normal Corda.
        numberOfSigners = 1
    }

    /**
     * This function is the verification function for the private transaction components as described in the metadata.
     * It returns a string of valid Zinc code. It should contain a `verify` function with the following prototype:
     *
     * ```
     * mod module_command_context;
     * use module_command_context::CommandContext;
     *
     * fn verify(ctx: CommandContext) {
     *     // Verifications go here.
     * }
     * ```
     *
     * To determine which types are available in the CommandContext (which contains all secret transaction components) for this command,
     * you can inspect the generated source directory at: `build/zinc/<command_name_in_camel_case>/src` and the
     * directory that describes the structure of the transaction component: `build/zinc/<command_name_in_camel_case>/structure`.
     * In the source directory look for the file `module_command_context.zn`, this file contains the zinc code for the CommandContext parameter.
     * In the structure directory you can find tree views of the separate transaction components in the Witness.
     * For example:
     * - module_outputs_<state_type_in_camel_case>_transaction_component.txt
     * - module_serialized_input_utxos_<state_type_in_camel_case>_transaction_component.txt
     * - module_serialized_reference_utxos_<state_type_in_camel_case>_transaction_component.txt
     */
    @Language("Rust")
    override fun verifyPrivate(): String {
        return """
                mod module_command_context;
                use module_command_context::CommandContext;

                fn verify(ctx: CommandContext) {
                    // Checks on structure are enforced by the transaction metadata. So no need to check here.
                    let input = ctx.inputs.example_token_0;
                    let output = ctx.outputs.example_token_0;
                    
                    // This also ensures equality of token type and other properties of the amount.
                    assert!(input.data.amount.equals(output.data.amount), "[MovePrivate] Amounts of input and output must equal");

                    assert!(input.data.amount.quantity > 0 as i64, "[MovePrivate] Quantity must be positive");

                    assert!(ctx.signers.contains(input.data.owner.public_key), "[MovePrivate] Input holder must sign");
                }

            """.trimIndent()
    }
}
