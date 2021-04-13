package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.testing.roundTrip
import com.ing.zknotary.testing.sameSize
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

        roundTrip(data1, serializersModule)
        sameSize(data1, data2, serializersModule)
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

        roundTrip(data1, serializersModule)
        sameSize(data1, data2, serializersModule)
    }
}
