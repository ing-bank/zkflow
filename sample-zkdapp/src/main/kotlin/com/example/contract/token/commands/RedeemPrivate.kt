package com.example.contract.token.commands

import com.example.contract.token.ExampleToken
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import org.intellij.lang.annotations.Language

@ZKP
class RedeemPrivate : ZKCommandData {
    override val metadata = commandMetadata {
        numberOfSigners = 2
        notary = true
        inputs {
            private(ExampleToken::class) at 0
        }
    }

    @Language("Rust")
    override fun verifyPrivate(): String {
        return """
            mod module_command_context;
            use module_command_context::CommandContext;

            fn verify(ctx: CommandContext) {
                let input = ctx.inputs.example_token_0;

                assert!(ctx.signers.contains(input.data.amount.issuer.public_key), "[RedeemPrivate] Issuer must sign");
                assert!(ctx.signers.contains(input.data.owner.public_key), "[RedeemPrivate] Holder must sign");
            }
        """.trimIndent()
    }
}