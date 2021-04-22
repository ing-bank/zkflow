package zinc.types

import com.ing.serialization.bfl.api.reified.serialize
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializedLinearPointerTest {
    private val log = loggerFor<DeserializedLinearPointerTest>()
    private val zincZKService = getZincZKService<DeserializedLinearPointerTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `a LinearPointer should be equal to itself`() {
        val data = Data(LinearPointer(getSomeId(), MyLinearState::class.java, true))
        val witness = toWitness(data)

        println(zincZKService.run(witness, ""))
    }

    private fun toWitness(item: Data): String {
        val bytes = serialize(item, serializersModule = CordaSerializers)
        return "{\"witness\":${bytes.toPrettyJSONArray()}}"
    }

    @Serializable
    private data class MyLinearState(
        override val participants: List<@Contextual AbstractParty>,
        override val linearId: @Contextual UniqueIdentifier
    ) : LinearState

    @Serializable
    private data class Data(
        val id: @Contextual LinearPointer<MyLinearState>
    )

    private fun getSomeId() = UniqueIdentifier("some.id", UUID(1L, 42L))
}
