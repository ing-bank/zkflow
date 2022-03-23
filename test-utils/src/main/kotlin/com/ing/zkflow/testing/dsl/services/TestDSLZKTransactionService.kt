package com.ing.zkflow.testing.dsl.services

import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.zkp.ZKTransactionService
import net.corda.core.transactions.WireTransaction

public interface TestDSLZKTransactionService : ZKTransactionService {
    public fun verify(wtx: WireTransaction, zkNetworkParameters: ZKNetworkParameters): SignedZKVerifierTransaction
}
