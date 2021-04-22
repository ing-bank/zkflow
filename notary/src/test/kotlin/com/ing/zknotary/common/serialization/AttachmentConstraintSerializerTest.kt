package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test

class AttachmentConstraintSerializerTest {
    @Serializable
    data class SimpleData(val value: AttachmentConstraint)

    @Serializable
    data class Data(@FixedLength([6]) val values: List<AttachmentConstraint>)

    private val serializersModule = CordaSerializers + CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.DEFAULT_SIGNATURE_SCHEME)

    @Test
    fun `serialize and deserialize simple inclusion AttachmentConstraint`() {
        val data1 = SimpleData(HashAttachmentConstraint(SecureHash.randomSHA256()))
        val data2 = SimpleData(AlwaysAcceptAttachmentConstraint)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }

    @Test
    fun `serialize and deserialize AttachmentConstraint`() {
        val data1 = Data(
            listOf(
                AlwaysAcceptAttachmentConstraint,
                HashAttachmentConstraint(SecureHash.randomSHA256()),
                WhitelistedByZoneAttachmentConstraint,
                AutomaticPlaceholderConstraint,
                SignatureAttachmentConstraint(TestIdentity.fresh("Alice").publicKey),
                AutomaticHashConstraint
            )
        )

        val data2 = Data(
            listOf(
                AlwaysAcceptAttachmentConstraint,
                WhitelistedByZoneAttachmentConstraint,
                AutomaticPlaceholderConstraint,
                AutomaticHashConstraint
            )
        )

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}
