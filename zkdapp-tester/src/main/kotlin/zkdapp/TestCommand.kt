package com.ing.zkflow.zktransaction

import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.transactionMetadata

class TestCommand: ZKTransactionMetadataCommandData {
    override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
        commands { +TestCommand::class }
    }

    @Transient
    override val metadata: ResolvedZKCommandMetadata = commandMetadata {
        private = true
        circuit { name = "TestCommand" }
        numberOfSigners = 1
    }
}
