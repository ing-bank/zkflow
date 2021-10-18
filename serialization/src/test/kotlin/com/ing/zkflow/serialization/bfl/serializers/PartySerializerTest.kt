package com.ing.zkflow.serialization.bfl.serializers

import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test

class PartySerializerTest {
    @Serializable
    data class Data(val value: @Contextual Party)

    @Test
    fun `serialize and deserialize Party`() {
        val data1 = Data(TestIdentity.fresh("Alice").party)
        val data2 = Data(TestIdentity.fresh("Bob").party)

        assertRoundTripSucceeds(data1)
        assertSameSize(data1, data2)
    }
}
