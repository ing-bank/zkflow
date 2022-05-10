package com.example.contract

import com.example.token.cbdc.CBDCToken
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.transactions.LedgerTransaction
import org.intellij.lang.annotations.Language

class CBDCContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.example.contract.CBDCContract"
    }

    interface  MoveInterface: Versioned
    @ZKP
    class Move : ZKCommandData, MoveInterface {
        override val metadata = commandMetadata {
            numberOfSigners = 2
            inputs {
                any(CBDCToken::class) at 0
            }
            outputs {
                private(CBDCToken::class) at 0
            }
            timeWindow = true
        }

        @Language("Rust")
        override fun verifyPrivate(): String {
            return """
                mod module_command_context;
                use module_command_context::CommandContext;

                fn verify(ctx: CommandContext) {
                    let input = ctx.inputs.cbdc_token_0;
                    let output = ctx.outputs.cbdc_token_0;
                    
                    assert!(input.data.amount.equals(output.data.amount), "[Move] Amounts of input and output must equal");

                    assert!(input.data.amount.quantity > 0 as i64, "[Move] Quantity must be positive");

                    assert!(ctx.signers.contains(input.data.holder.public_key), "[Move] Owner must sign");
                    assert!(ctx.signers.contains(output.data.holder.public_key), "[Move] Receiver must sign");
                }

            """.trimIndent()
        }
    }

    interface  IssueInterface: Versioned
    @ZKP
    class Issue : ZKCommandData, IssueInterface {
        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs {
                private(CBDCToken::class) at 0
            }
            timeWindow = true
        }

        @Language("Rust")
        override fun verifyPrivate(): String {
            return """
                mod module_command_context;
                use module_command_context::CommandContext;

                fn verify(ctx: CommandContext) {
                    let output = ctx.outputs.cbdc_token_0;
                    
                    assert!(output.data.amount.quantity > 0 as i64, "[Issue] Quantity must be positive");

                    assert!(ctx.signers.contains(output.data.holder.public_key), "[Issue] Owner must sign");
                }
            """.trimIndent()
        }
    }

    interface  SplitInterface: Versioned
    @ZKP
    class Split : ZKCommandData, SplitInterface {
        override val metadata = commandMetadata {
            numberOfSigners = 2
            inputs {
                any(CBDCToken::class) at 0
            }
            outputs {
                private(CBDCToken::class) at 0
                private(CBDCToken::class) at 1
            }
            timeWindow = true
        }

        @Language("Rust")
        override fun verifyPrivate(): String {
            return """
                mod module_command_context;
                use module_command_context::CommandContext;

                fn verify(ctx: CommandContext) {
                    let input = ctx.inputs.cbdc_token_0;
                    let output_0 = ctx.outputs.cbdc_token_0;
                    let output_1 = ctx.outputs.cbdc_token_1;
                    
                    assert!(input.data.amount.quantity > 0 as i64, "[Split] Quantity must be positive");
                    assert!(output_0.data.amount.quantity > 0 as i64, "[Split] Quantity must be positive");
                    assert!(output_1.data.amount.quantity > 0 as i64, "[Split] Quantity must be positive");
                    
                    assert!(output_0.data.amount.quantity + output_1.data.amount.quantity == input.data.amount.quantity, "[Split] Amounts of funds must be constant");
                    
                    assert!(ctx.signers.contains(input.data.holder.public_key), "[Split] Owner must sign");
                }
            """.trimIndent()
        }
    }

    override fun verify(tx: LedgerTransaction) {
        // Contract verifications go here
    }
}