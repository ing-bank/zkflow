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
                mod command_context;
                use command_context::CommandContext;

                fn verify(ctx: CommandContext) {
                    let input = ctx.inputs.mock_asset_contract_mock_asset_0;
                    let output = ctx.outputs.mock_asset_contract_mock_asset_0;
                    assert!(input.data.value == output.data.value, "[Move] Values of input and output must equal");

                    assert!(ctx.signers.contains(input.data.owner.public_key), "[Move] Owner must sign");
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
                mod command_context;
                use command_context::CommandContext;

                fn verify(ctx: CommandContext) {
                    let tx_mock_asset = ctx.outputs.mock_asset_contract_mock_asset_0;

                    assert!(ctx.signers.contains(tx_mock_asset.data.owner.public_key), "[Issue] Owner must sign");
                }
            """.trimIndent()
        }
    }

    override fun verify(tx: LedgerTransaction) {
        // Contract verifications go here
    }
}
