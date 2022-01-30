package zkdapp

import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.transactionMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class TestCommand: ZKTransactionMetadataCommandData {
    @Transient
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
