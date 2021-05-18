package com.ing.zknotary.notary.transactions

import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import com.ing.zknotary.testing.shouldHaveSamePublicApiAs
import net.corda.core.transactions.TransactionBuilder
import org.junit.jupiter.api.Test

class ZKTransactionBuilderTest {
    @Test
    fun `Public API of ZKTransactionBuilder equals TransactionBuilder`() {
        ZKTransactionBuilder::class.shouldHaveSamePublicApiAs(
            TransactionBuilder::class,
            listOf("addOutputState")
        )
    }
}
