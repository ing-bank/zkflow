package com.example.contract.token.commands

import com.example.contract.token.ExampleToken
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import org.intellij.lang.annotations.Language

@ZKP
class SplitPrivate : ZKCommandData {
    override val metadata = commandMetadata {
        numberOfSigners = 1
        notary = true
        inputs {
            private(ExampleToken::class) at 0
        }
        outputs {
            private(ExampleToken::class) at 0
            private(ExampleToken::class) at 1
        }
        timeWindow = true
    }

    @Language("Rust")
    override fun verifyPrivate(): String {
        return """
            mod module_command_context;
            use module_command_context::CommandContext;

            fn verify(ctx: CommandContext) {
                let input = ctx.inputs.example_token_0;
                let output_0 = ctx.outputs.example_token_0;
                let output_1 = ctx.outputs.example_token_1;
                
                assert!(input.data.amount.quantity > 0 as i64, "[Split] Input quantity must be positive");
                assert!(output_0.data.amount.quantity > 0 as i64, "[Split] Output 0 quantity must be positive");
                assert!(output_1.data.amount.quantity > 0 as i64, "[Split] Output 1 quantity must be positive");
                
                assert!(output_0.data.amount.quantity + output_1.data.amount.quantity == input.data.amount.quantity, "[Split] Amounts of funds must be constant");
                
                assert!(ctx.signers.contains(input.data.owner.public_key), "[Split] Input holder must sign");
            }
        """.trimIndent()
    }
}