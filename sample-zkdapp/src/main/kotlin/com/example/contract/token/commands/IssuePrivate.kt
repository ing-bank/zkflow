package com.example.contract.token.commands

import com.example.contract.token.ExampleToken
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import org.intellij.lang.annotations.Language

@ZKP
class IssuePrivate : ZKCommandData {
    override val metadata = commandMetadata {
        numberOfSigners = 1
        outputs {
            private(ExampleToken::class) at 0
        }
    }

    @Language("Rust")
    override fun verifyPrivate(): String {
        return """
            mod module_command_context;
            use module_command_context::CommandContext;

            fn verify(ctx: CommandContext) {
                // Checks on structure are enforced by the transaction metadata. So no need to check here.
                let output = ctx.outputs.example_token_0;
                
                assert!(output.data.amount.quantity > 0 as i64, "[IssuePrivate] Quantity must be positive");

                assert!(ctx.signers.contains(output.data.amount.token.issuer.public_key), "[IssuePrivate] Issuer must sign");
            }
        """.trimIndent()
    }
}