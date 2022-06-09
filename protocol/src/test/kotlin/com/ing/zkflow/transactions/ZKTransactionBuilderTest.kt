package com.ing.zkflow.transactions

import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.testing.shouldHaveSamePublicApiAs
import net.corda.core.transactions.TransactionBuilder
import org.junit.Test

class ZKTransactionBuilderTest {
    @Test
    fun `Public API of ZKTransactionBuilder equals TransactionBuilder`() {
        ZKTransactionBuilder::class.shouldHaveSamePublicApiAs(
            TransactionBuilder::class,
            listOf(
                // "addOutputState",
                "lockId", // Not clear what it is used for
                "<init>" // constructors may be different
            )
        )
    }
}
