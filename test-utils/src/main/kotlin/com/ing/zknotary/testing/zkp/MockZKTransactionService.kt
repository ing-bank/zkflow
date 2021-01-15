package com.ing.zknotary.testing.zkp

import com.ing.zknotary.common.zkp.ZKService
import com.ing.zknotary.common.zkp.ZKTransactionCordaService
import net.corda.core.crypto.SecureHash
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService

@CordaService
public open class MockZKTransactionService(serviceHub: AppServiceHub) : ZKTransactionCordaService(serviceHub) {

    private val zkService: ZKService = MockZKService()

    override fun zkServiceForTx(circuitId: SecureHash): ZKService = zkService
}
