package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test

class AnonymousPartySerializerTest {
    @Serializable
    data class Data(val value: @Contextual AnonymousParty)

    @Test
    fun `serialize and deserialize Party`() {
        val serializersModule = CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.DEFAULT_SIGNATURE_SCHEME)
        val data1 = Data(TestIdentity.fresh("Alice").party.anonymise())
        val data2 = Data(TestIdentity.fresh("Bob").party.anonymise())

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}
