@file:Suppress("DEPRECATION")

package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.annotations.corda.SHA256
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash

@ZKP
data class WrapsAttachmentConstraints(
    val alwaysAcceptAttachmentConstraint: AlwaysAcceptAttachmentConstraint = AlwaysAcceptAttachmentConstraint,
    val hashAttachmentConstraintSHA256Explicit: @SHA256 HashAttachmentConstraint = HashAttachmentConstraint(SecureHash.zeroHash),
    val whitelistedByZoneAttachmentConstraint: WhitelistedByZoneAttachmentConstraint = WhitelistedByZoneAttachmentConstraint,
    val automaticHashConstraint: AutomaticHashConstraint = AutomaticHashConstraint,
    val automaticPlaceholderConstraint: AutomaticPlaceholderConstraint = AutomaticPlaceholderConstraint,
    val signatureAttachmentConstraint: @EdDSA SignatureAttachmentConstraint = SignatureAttachmentConstraint(
        PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
    ),
)
