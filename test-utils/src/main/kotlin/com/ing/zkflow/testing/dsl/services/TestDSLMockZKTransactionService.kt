package com.ing.zkflow.testing.dsl.services

import com.ing.zkflow.common.zkp.AbstractZKTransactionService
import com.ing.zkflow.common.zkp.ZKService
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.crypto.blake2s256
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.testing.zkp.MockZKService
import net.corda.core.crypto.DigestService
import net.corda.core.node.ServiceHub

public open class TestDSLMockZKTransactionService(
    serviceHub: ServiceHub,
    zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage
) : AbstractZKTransactionService(serviceHub) {
    override val vtxStorage: ZKVerifierTransactionStorage = zkVerifierTransactionStorage

    override fun zkServiceForCommandMetadata(metadata: ResolvedZKCommandMetadata): ZKService = MockZKService(DigestService.blake2s256)

    override fun setup(command: ResolvedZKCommandMetadata, force: Boolean) {
        // Do nothing
    }
}
