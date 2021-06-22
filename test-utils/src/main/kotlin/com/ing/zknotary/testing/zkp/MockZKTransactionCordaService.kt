package com.ing.zknotary.testing.zkp

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.crypto.zinc
import com.ing.zknotary.common.crypto.zincAlgorithm
import com.ing.zknotary.common.zkp.AbstractZKTransactionService
import com.ing.zknotary.common.zkp.ZKService
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

    private val zkService: ZKService = MockZKService(serviceHub, DigestService.zinc)

    override fun zkServiceForCommand(command: ZKCommandData): ZKService = zkService
}
