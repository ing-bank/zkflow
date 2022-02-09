package zkdapp

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import kotlinx.serialization.Serializable

@Serializable
class TestCommand : ZKCommandData {
    @Transient
    override val metadata: ResolvedZKCommandMetadata = commandMetadata {
        circuit { name = "TestCommand" }
        numberOfSigners = 1
    }
}
