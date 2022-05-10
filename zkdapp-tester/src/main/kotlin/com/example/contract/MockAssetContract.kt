package com.example.contract

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.versioning.ZincUpgrade
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import org.intellij.lang.annotations.Language
import java.util.Random


class MockAssetContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.example.contract.MockAssetContract"
    }

    interface  VersionedMockAsset: Versioned

    @ZKP
    @BelongsToContract(MockAssetContract::class)
    data class MockAsset(
        override val owner: @EdDSA AnonymousParty,
        val value: Int = Random().nextInt()
    ) : VersionedMockAsset, ZKOwnableState {
        @ZincUpgrade("Self::new(previous_version.owner, 0 as i32)")
        constructor(previousVersion: MockAssetV1): this(previousVersion.owner, 0)

        override val participants: List<AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(MovePublicToPrivate(), copy(owner = newOwner))
    }

    @ZKP
    @BelongsToContract(MockAssetContract::class)
    data class MockAssetV1(
        override val owner: @EdDSA AnonymousParty,
    ) : VersionedMockAsset, ZKOwnableState {
        override val participants: List<AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(MovePublicToPrivate(), copy(owner = newOwner))
    }

    interface  VersionedMovePublicToPrivate: Versioned
    @ZKP
    class MovePublicToPrivate : VersionedMovePublicToPrivate, ZKCommandData {
        override val metadata = commandMetadata {
            numberOfSigners = 2
            inputs {
                any(MockAsset::class) at 0
            }
            outputs {
                private(MockAsset::class) at 0
            }
            timeWindow = true
        }

        @Language("Rust")
        override fun verifyPrivate(): String {
            return """
                mod module_command_context;
                use module_command_context::CommandContext;

                fn verify(ctx: CommandContext) {
                    let input = ctx.inputs.mock_asset_contract_mock_asset_0;
                    let output = ctx.outputs.mock_asset_contract_mock_asset_0;
                    assert!(input.data.value == output.data.value, "[Move] Values of input and output must equal");

                    assert!(ctx.signers.contains(input.data.owner.public_key), "[Move] Owner must sign");
                }

            """.trimIndent()
        }
    }

    interface  VersionedIssuePrivate: Versioned
    @ZKP
    class IssuePrivate : VersionedIssuePrivate, ZKCommandData {
        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs {
                private(MockAsset::class) at 0
            }
        }

        @Language("Rust")
        override fun verifyPrivate(): String {
            return """
                mod module_command_context;
                use module_command_context::CommandContext;

                fn verify(ctx: CommandContext) {
                    let tx_mock_asset = ctx.outputs.mock_asset_contract_mock_asset_0;

                    assert!(ctx.signers.contains(tx_mock_asset.data.owner.public_key), "[Issue] Owner must sign");
                }
            """.trimIndent()
        }
    }

    override fun verify(tx: LedgerTransaction) {
        // Public contract verifications go here. N/A in this case
    }
}
