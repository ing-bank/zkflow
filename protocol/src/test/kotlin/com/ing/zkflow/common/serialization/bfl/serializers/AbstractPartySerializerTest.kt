package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.zkflow.testing.assertRoundTripSucceeds
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.identity.AbstractParty
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AbstractPartySerializerTest {
    @Serializable
    data class Data(val value: @Polymorphic AbstractParty)

    @Serializable
    data class ListData(@FixedLength([3]) val myList: List<@Polymorphic AbstractParty>)

    @Test
    fun `serialize and deserialize Party`() {
        val data1 = Data(TestIdentity.fresh("Alice").party)
        val data2 = Data(TestIdentity.fresh("Bob").party.anonymise())

        assertRoundTripSucceeds(data1)
        assertRoundTripSucceeds(data2)
    }

    @Test
    fun `different implementations of the same abstract class should not coexist in a collection`() {
        assertThrows<SerdeError.DifferentPolymorphicImplementations> {
            serialize(
                ListData(
                    listOf(
                        TestIdentity.fresh("Alice").party,
                        TestIdentity.fresh("Bob").party.anonymise()
                    )
                ),
                serializersModule = CordaSerializers.module
            )
        }
    }
}
