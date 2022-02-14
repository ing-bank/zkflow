package com.example.network

import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.ZKNetworkParametersProvider
import com.ing.zkflow.common.network.ZKNotaryInfo
import com.ing.zkflow.common.zkp.ZKFlow
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme

class ZKDappTesterZKNetworkParamsProvider : ZKNetworkParametersProvider {
    override val parameters: ZKNetworkParameters = object : ZKNetworkParameters {
        override val participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
        override val attachmentConstraintType: ZKAttachmentConstraintType = ZKFlow.DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT_TYPE
        override val notaryInfo: ZKNotaryInfo = ZKNotaryInfo(ZKFlow.DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME)
        override val digestAlgorithm: DigestAlgorithm = ZKFlow.DEFAULT_ZKFLOW_DIGEST_IDENTIFIER
        override val serializationSchemeId: Int = ZKFlow.DEFAULT_SERIALIZATION_SCHEME_ID
    }
}