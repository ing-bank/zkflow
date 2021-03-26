package com.ing.zknotary.testing.zkp

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.crypto.zinc
import com.ing.zknotary.common.crypto.zincAlgorithm
import com.ing.zknotary.common.zkp.ZKService
import com.ing.zknotary.common.zkp.ZKTransactionCordaService
import net.corda.core.crypto.DigestService
import net.corda.core.internal.HashAgility
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService

@CordaService
public open class MockZKTransactionService(serviceHub: AppServiceHub) : ZKTransactionCordaService(serviceHub) {

    init {
        HashAgility.init(zincAlgorithm)
    }

    private val zkService: ZKService = MockZKService(DigestService.zinc)

    override fun zkServiceForCommand(command: ZKCommandData): ZKService = zkService
}
