package com.ing.zkflow.testing.zkp

import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.ZKNotaryInfo
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme

public object MockZKNetworkParameters : ZKNetworkParameters {
    override val participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    /**
     * Here we don't use the preferred SignatureAttachmentConstraint, because HashAttachment place nicer with the
     * standard constraint logic for transactions built with the TransactionBuilder and DSL. That turns
     * `AutomaticPlaceholderConstraint` into `HashAttachmentConstraint` in most cases.
     */
    override val attachmentConstraintType: ZKAttachmentConstraintType =
        ZKAttachmentConstraintType.HashAttachmentConstraintType(DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM)
    override val notaryInfo: ZKNotaryInfo = ZKNotaryInfo(ZKFlow.DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME)
    override val digestAlgorithm: DigestAlgorithm = ZKFlow.DEFAULT_ZKFLOW_DIGEST_IDENTIFIER
    override val serializationSchemeId: Int = ZKFlow.DEFAULT_SERIALIZATION_SCHEME_ID
}
