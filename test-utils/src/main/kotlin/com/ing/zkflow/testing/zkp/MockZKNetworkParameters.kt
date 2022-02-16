package com.ing.zkflow.testing.zkp

import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.ZKNotaryInfo
import com.ing.zkflow.common.zkp.ZKFlow
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme

public data class MockZKNetworkParameters(
    override val participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME,

    /**
     * Here we don't use the preferred SignatureAttachmentConstraint, because AlwaysAcceptAttachmentConstraintType place nicer with the
     * standard constraint logic for transactions built with the test DSL.
     */
    override val attachmentConstraintType: ZKAttachmentConstraintType = ZKAttachmentConstraintType.AlwaysAcceptAttachmentConstraintType,
    // ZKAttachmentConstraintType.HashAttachmentConstraintType(DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM),
    override val notaryInfo: ZKNotaryInfo = ZKNotaryInfo(ZKFlow.DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME),
    override val digestAlgorithm: DigestAlgorithm = ZKFlow.DEFAULT_ZKFLOW_DIGEST_IDENTIFIER,
    override val serializationSchemeId: Int = ZKFlow.DEFAULT_SERIALIZATION_SCHEME_ID
) : ZKNetworkParameters
