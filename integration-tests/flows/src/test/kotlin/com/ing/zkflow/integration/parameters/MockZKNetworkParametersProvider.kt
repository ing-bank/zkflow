package com.ing.zkflow.integration.parameters

import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.ZKNetworkParametersProvider
import com.ing.zkflow.common.network.ZKNotaryInfo
import com.ing.zkflow.common.zkp.ZKFlow
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme

class MockZKNetworkParametersProvider : ZKNetworkParametersProvider {
    override val parameters: ZKNetworkParameters = object : ZKNetworkParameters {
        override val participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME

        /**
         * Here we don't use the preferred SignatureAttachmentConstraint, because HashAttachment place nicer with the
         * standard constraint logic for transactions built with the TransactionBuilder and DSL. That turns
         * `AutomaticPlaceholderConstraint` into `HashAttachmentConstraint` in most cases.
         */
        override val attachmentConstraintType: ZKAttachmentConstraintType =
            ZKAttachmentConstraintType.HashAttachmentConstraintType(ZKFlow.DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM)
        override val notaryInfo: ZKNotaryInfo = ZKNotaryInfo(ZKFlow.DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME)
        override val digestAlgorithm: DigestAlgorithm = ZKFlow.DEFAULT_ZKFLOW_DIGEST_IDENTIFIER
        override val serializationSchemeId: Int = ZKFlow.DEFAULT_SERIALIZATION_SCHEME_ID
    }
}
