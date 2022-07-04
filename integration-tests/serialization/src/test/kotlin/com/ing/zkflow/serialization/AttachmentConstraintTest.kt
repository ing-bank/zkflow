@file:Suppress("DEPRECATION")

package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.annotations.corda.SHA256
import com.ing.zkflow.annotations.corda.SHA256DigestAlgorithm
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.corda.AlwaysAcceptAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticHashConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticPlaceholderConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.HashAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import com.ing.zkflow.serialization.serializer.corda.SignatureAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.WhitelistedByZoneAttachmentConstraintSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class AttachmentConstraintTest : SerializerTest {
    // Setup
    @ZKP
    data class AttachmentConstraints(
        val alwaysAcceptAttachmentConstraint: AlwaysAcceptAttachmentConstraint = AlwaysAcceptAttachmentConstraint,
        val hashAttachmentConstraint: @SHA256 HashAttachmentConstraint = HashAttachmentConstraint(SecureHash.zeroHash),
        val whitelistedByZoneAttachmentConstraint: WhitelistedByZoneAttachmentConstraint = WhitelistedByZoneAttachmentConstraint,
        val automaticHashConstraint: AutomaticHashConstraint = AutomaticHashConstraint,
        val automaticPlaceholderConstraint: AutomaticPlaceholderConstraint = AutomaticPlaceholderConstraint,
        val signatureAttachmentConstraint: @EdDSA SignatureAttachmentConstraint = SignatureAttachmentConstraint(
            PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
        ),
    )

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class AttachmentConstraintsResolved(
        @Serializable(with = AlwaysAcceptAttachmentConstraint_0::class) val alwaysAcceptAttachmentConstraint: @Contextual AlwaysAcceptAttachmentConstraint = AlwaysAcceptAttachmentConstraint,
        @Serializable(with = HashAttachmentConstraint_0::class) val hashAttachmentConstraint: @Contextual HashAttachmentConstraint = HashAttachmentConstraint(SecureHash.zeroHash),
        @Serializable(with = WhitelistedByZoneAttachmentConstraint_0::class) val whitelistedByZoneAttachmentConstraint: @Contextual WhitelistedByZoneAttachmentConstraint = WhitelistedByZoneAttachmentConstraint,
        @Serializable(with = AutomaticHashConstraint_0::class) val automaticHashConstraint: @Contextual AutomaticHashConstraint = AutomaticHashConstraint,
        @Serializable(with = AutomaticPlaceholderConstraint_0::class) val automaticPlaceholderConstraint: @Contextual AutomaticPlaceholderConstraint = AutomaticPlaceholderConstraint,
        @Serializable(with = SignatureAttachmentConstraint_0::class) val signatureAttachmentConstraint: @Contextual SignatureAttachmentConstraint = SignatureAttachmentConstraint(
            PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
        ),
    ) {
        object AlwaysAcceptAttachmentConstraint_0 :
            WrappedFixedLengthKSerializerWithDefault<AlwaysAcceptAttachmentConstraint>(
                AlwaysAcceptAttachmentConstraintSerializer
            )

        object HashAttachmentConstraint_0 : HashAttachmentConstraintSerializer(SHA256DigestAlgorithm::class)

        object WhitelistedByZoneAttachmentConstraint_0 :
            WrappedFixedLengthKSerializerWithDefault<WhitelistedByZoneAttachmentConstraint>(
                WhitelistedByZoneAttachmentConstraintSerializer
            )
        object AutomaticHashConstraint_0 : WrappedFixedLengthKSerializerWithDefault<AutomaticHashConstraint>(
            AutomaticHashConstraintSerializer
        )
        object AutomaticPlaceholderConstraint_0 : WrappedFixedLengthKSerializerWithDefault<AutomaticPlaceholderConstraint>(
            AutomaticPlaceholderConstraintSerializer
        )
        object SignatureAttachmentConstraint_0 : SignatureAttachmentConstraintSerializer(4)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `AttachmentConstraints makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(AttachmentConstraintTest_AttachmentConstraints_Serializer, AttachmentConstraints())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `AttachmentConstraints generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            AttachmentConstraintsResolved.serializer(),
            AttachmentConstraintsResolved()
        ) shouldBe
            engine.serialize(AttachmentConstraintTest_AttachmentConstraints_Serializer, AttachmentConstraints())
    }
}
