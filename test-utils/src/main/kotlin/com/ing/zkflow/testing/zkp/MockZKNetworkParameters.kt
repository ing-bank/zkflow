package com.ing.zkflow.testing.zkp

import com.ing.zkflow.common.network.DebugSettings
import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.ZKNotaryInfo
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.common.zkp.ZKFlow
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme
import java.nio.file.Files
import java.nio.file.Paths

public data class MockZKNetworkParameters(
    override val version: Int = Int.MIN_VALUE, // As low as possible, so we never get automatically selected as the latest version by ZKNetworkParametersServiceLoader
    override val participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME,

    /**
     * Here we don't use the preferred SignatureAttachmentConstraint, because AlwaysAcceptAttachmentConstraintType plays nicer with the
     * standard constraint logic for transactions built with the test DSL.
     */
    override val attachmentConstraintType: ZKAttachmentConstraintType = ZKAttachmentConstraintType.AlwaysAcceptAttachmentConstraintType,
    override val notaryInfo: ZKNotaryInfo = ZKNotaryInfo(ZKFlow.DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME),
    override val digestAlgorithm: DigestAlgorithm = ZKFlow.DEFAULT_ZKFLOW_DIGEST_IDENTIFIER,
    override val serializationSchemeId: Int = BFLSerializationScheme.SCHEME_ID,
    override val debugSettings: DebugSettings = object : DebugSettings {
        override val dumpSerializationStructure = true
        override fun debugDirectory() = Paths.get("build/debug-structure").also { Files.createDirectories(it) }
    }
) : ZKNetworkParameters
