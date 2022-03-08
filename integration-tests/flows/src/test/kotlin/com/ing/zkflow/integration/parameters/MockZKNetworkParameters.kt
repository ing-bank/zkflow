package com.ing.zkflow.integration.parameters

import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.ZKNotaryInfo
import com.ing.zkflow.common.zkp.ZKFlow
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme

class MockFLowTestZKNetworkParameters : ZKNetworkParameters {
    override val version: Int = 12
    override val participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    /**
     * Here we don't use the preferred SignatureAttachmentConstraint, because HashAttachmentConstraint is what AutomaticPlaceholderConstraint
     * resolves to when a WireTransaction is created in the context of a Flow test.
     */
    override val attachmentConstraintType: ZKAttachmentConstraintType =
        ZKAttachmentConstraintType.HashAttachmentConstraintType(ZKFlow.DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM)
    override val notaryInfo: ZKNotaryInfo = ZKNotaryInfo(ZKFlow.DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME)
    override val digestAlgorithm: DigestAlgorithm = ZKFlow.DEFAULT_ZKFLOW_DIGEST_IDENTIFIER
    override val serializationSchemeId: Int = ZKFlow.DEFAULT_SERIALIZATION_SCHEME_ID
}
