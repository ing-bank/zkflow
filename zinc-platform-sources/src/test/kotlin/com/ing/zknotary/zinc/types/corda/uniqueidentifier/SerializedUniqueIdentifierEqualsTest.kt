package com.ing.zknotary.zinc.types.corda.uniqueidentifier

import com.ing.serialization.bfl.api.reified.serialize
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.testing.toJsonArray
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.UniqueIdentifier
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.ExperimentalTime

@ExperimentalTime
class SerializedUniqueIdentifierEqualsTest {
    private val zincZKService = getZincZKService<SerializedUniqueIdentifierEqualsTest>()

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
        val expected = "${data1 == data2}"

        zincZKService.run(witness, expected)
    }

    private fun toWitness(left: Data, right: Data): String {
        val bytes = serialize(left, serializersModule = CordaSerializers.module) +
            serialize(right, serializersModule = CordaSerializers.module)
        return buildJsonObject {
            put("witness", bytes.toJsonArray())
        }.toString()
    }

    @Serializable
    private data class Data(
        val id: @Contextual UniqueIdentifier
    )
}
