package com.ing.zkflow.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.crypto.SecureHash
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class AttachmentConstraintSerializerTest {
    @Serializable
    data class SimpleData(val value: AttachmentConstraint)

    @Serializable
    data class Data(@FixedLength([6]) val values: List<AttachmentConstraint>)

    companion object {
        @JvmStatic
        fun simpleDataPairs() = listOf(
            Pair(
                SimpleData(HashAttachmentConstraint(SecureHash.randomSHA256())),
                SimpleData(HashAttachmentConstraint(SecureHash.randomSHA256()))
            ),
            Pair(
                SimpleData(SignatureAttachmentConstraint(TestIdentity.fresh("Bob").publicKey)),
                SimpleData(SignatureAttachmentConstraint(TestIdentity.fresh("Alice").publicKey)),
            ),
            Pair(SimpleData(AlwaysAcceptAttachmentConstraint), SimpleData(WhitelistedByZoneAttachmentConstraint)),
            Pair(SimpleData(WhitelistedByZoneAttachmentConstraint), SimpleData(AutomaticPlaceholderConstraint)),
            Pair(SimpleData(AutomaticPlaceholderConstraint), SimpleData(WhitelistedByZoneAttachmentConstraint)),
        )

        @JvmStatic
        fun dataPairs() = listOf(
            Pair(
                Data(listOf(HashAttachmentConstraint(SecureHash.randomSHA256()))),
                Data(
                    listOf(
                        HashAttachmentConstraint(SecureHash.randomSHA256()),
                        HashAttachmentConstraint(SecureHash.randomSHA256()),
                    )
                )
            ),
            Pair(
                Data(
                    listOf(
                        SignatureAttachmentConstraint(TestIdentity.fresh("Bob").publicKey),
                        SignatureAttachmentConstraint(TestIdentity.fresh("Alice").publicKey),
                    )
                ),
                Data(listOf(SignatureAttachmentConstraint(TestIdentity.fresh("Alice").publicKey)))
            ),
            Pair(Data(listOf(AlwaysAcceptAttachmentConstraint)), Data(listOf(WhitelistedByZoneAttachmentConstraint))),
            Pair(Data(listOf(WhitelistedByZoneAttachmentConstraint)), Data(listOf(AutomaticPlaceholderConstraint))),
            Pair(Data(listOf(AutomaticPlaceholderConstraint)), Data(listOf(WhitelistedByZoneAttachmentConstraint))),
        )
    }

    @ParameterizedTest
    @MethodSource("simpleDataPairs")
    fun `serialize and deserialize simple inclusion AttachmentConstraint`(simpleDataPair: Pair<SimpleData, SimpleData>) {
        assertRoundTripSucceeds(simpleDataPair.first)
        assertSameSize(simpleDataPair.first, simpleDataPair.second)
    }

    @ParameterizedTest
    @MethodSource("dataPairs")
    fun `serialize and deserialize AttachmentConstraint`(dataPair: Pair<Data, Data>) {
        assertRoundTripSucceeds(dataPair.first)
        assertSameSize(dataPair.first, dataPair.second)
    }
}
