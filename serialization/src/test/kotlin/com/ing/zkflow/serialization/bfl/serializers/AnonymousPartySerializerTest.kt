package com.ing.zkflow.serialization.bfl.serializers

import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.identity.AnonymousParty
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test

class AnonymousPartySerializerTest {
    @Serializable
    data class Data(val value: @Contextual AnonymousParty)

    @Test
    fun `serialize and deserialize Party`() {
        val data1 = Data(TestIdentity.fresh("Alice").party.anonymise())
        val data2 = Data(TestIdentity.fresh("Bob").party.anonymise())

        assertRoundTripSucceeds(data1)
        assertSameSize(data1, data2)
    }
}
