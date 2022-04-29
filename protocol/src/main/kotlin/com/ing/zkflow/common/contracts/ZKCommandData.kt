package com.ing.zkflow.common.contracts

import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import org.intellij.lang.annotations.Language

interface ZKCommandData : CommandData {
    val metadata: ResolvedZKCommandMetadata

    @Language("Rust")
    fun verifyPrivate(): String =
        """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                assert!(true != false, "Reality is in an inconsistent state.");
            } 
        """.trimIndent()
}

fun <T : CommandData> CommandWithParties<T>.toZKCommand(): Command<ZKCommandData> {
    require(value is ZKCommandData) { "CommandData must implement ZKCommandData" }
    return Command(value as ZKCommandData, signers)
}
