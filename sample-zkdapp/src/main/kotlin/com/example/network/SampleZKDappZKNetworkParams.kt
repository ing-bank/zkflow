package com.example.network

import com.ing.zkflow.common.network.DebugSettings
import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.ZKNotaryInfo
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.common.zkp.ZKFlow
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme
import java.nio.file.Files

class SampleZKDappZKNetworkParams : ZKNetworkParameters {
    override val version: Int = 1
    override val participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
    override val attachmentConstraintType: ZKAttachmentConstraintType =
        ZKAttachmentConstraintType.HashAttachmentConstraintType()
    override val notaryInfo: ZKNotaryInfo = ZKNotaryInfo(ZKFlow.DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME)
    override val digestAlgorithm: DigestAlgorithm = ZKFlow.DEFAULT_ZKFLOW_DIGEST_IDENTIFIER
    override val serializationSchemeId: Int = BFLSerializationScheme.SCHEME_ID
    override val debugSettings: DebugSettings = object : DebugSettings {
        override val dumpSerializationStructure = false
        override fun debugDirectory() = Files.createTempDirectory("sample-zkdapp-runtime-debug-")
    }
}
