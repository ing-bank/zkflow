package zinc.types

import com.ing.serialization.bfl.api.reified.serialize
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.ExperimentalTime

@ExperimentalTime
class SerializedUniqueIdentifierEqualsTest {
    private val log = loggerFor<SerializedUniqueIdentifierEqualsTest>()
    private val zincZKService = getZincZKService<SerializedUniqueIdentifierEqualsTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `a UniqueIdentifier should be equal to itself`() {
        val data = Data(UniqueIdentifier("some.id", UUID(1L, 42L)))
        performEqualityTest(data, data)
    }

    @Test
    fun `two UniqueIdentifiers should not be equal when the id is different`() {
        val data1 = Data(UniqueIdentifier("some.id", UUID(1L, 42L)))
        val data2 = Data(UniqueIdentifier("some.id", UUID(1L, 43L)))
        performEqualityTest(data1, data2)
    }

    @Test
    fun `two UniqueIdentifiers should be equal when the id is equal, but not the externalId`() {
        val data1 = Data(UniqueIdentifier("some.id", UUID(1L, 42L)))
        val data2 = Data(UniqueIdentifier("some.other.id", UUID(1L, 42L)))
        performEqualityTest(data1, data2)
    }

    @Test
    fun `two UniqueIdentifiers should be equal to when the id is equal, with absent externalIds`() {
        val data1 = Data(UniqueIdentifier("some.id", UUID(1L, 42L)))
        val data2 = Data(UniqueIdentifier(id = UUID(1L, 42L)))
        performEqualityTest(data1, data2)
    }

    private fun performEqualityTest(
        data1: Data,
        data2: Data
    ) {
        val witness = toWitness(data1, data2)
        val areEqual = data1.id == data2.id
        val expected = "$areEqual"

        zincZKService.proveTimed(witness, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    private fun toWitness(left: Data, right: Data): String {
        val bytes = serialize(left, serializersModule = CordaSerializers) +
            serialize(right, serializersModule = CordaSerializers)
        return "{\"witness\":${bytes.toJsonArray()}}"
    }

    @Serializable
    private data class Data(
        val id: @Contextual UniqueIdentifier
    )
}
