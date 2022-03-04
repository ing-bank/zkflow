package com.ing.zkflow.testing.zkp

import com.ing.zkflow.common.zkp.AbstractZKTransactionService
import com.ing.zkflow.common.zkp.ZKService
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.crypto.zinc
import com.ing.zkflow.crypto.zincAlgorithm
import net.corda.core.crypto.DigestService
import net.corda.core.internal.HashAgility
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService

@CordaService
public open class MockZKTransactionCordaService(serviceHub: AppServiceHub) : MockZKTransactionService(serviceHub)

public open class MockZKTransactionService(serviceHub: ServiceHub) : AbstractZKTransactionService(serviceHub) {

    init {
        HashAgility.init(zincAlgorithm)
    }

    override fun zkServiceForCommandMetadata(metadata: ResolvedZKCommandMetadata): ZKService =
        MockZKService(DigestService.zinc)

    override fun setup(command: ResolvedZKCommandMetadata, force: Boolean) {
        // Do nothing
    }
}
