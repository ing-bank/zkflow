package com.example.contract.token.commands

import com.example.contract.token.ExampleToken
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import org.intellij.lang.annotations.Language

@ZKP
class MovePrivate : ZKCommandData {
    override val metadata = commandMetadata {
        numberOfSigners = 1
        notary = true
        inputs {
            private(ExampleToken::class) at 0
        }
        outputs {
            private(ExampleToken::class) at 0
        }
        timeWindow = true
    }

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
