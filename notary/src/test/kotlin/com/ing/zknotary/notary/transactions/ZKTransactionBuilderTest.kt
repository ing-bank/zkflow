package com.ing.zknotary.notary.transactions

import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import net.corda.core.transactions.TransactionBuilder
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

class ZKTransactionBuilderTest {
    @Test
    fun `Public API of ZKTransactionBuilder equals TransactionBuilder`() {
        ZKTransactionBuilder::class shouldHaveSamePublicApiAs TransactionBuilder::class
    }
}

private infix fun KClass<*>.shouldHaveSamePublicApiAs(expected: KClass<*>) {
    println("THIS: $this")
    println("EXPECTED: $expected")
    val actualMemberFunctions = this.memberFunctions.filter { it.visibility == KVisibility.PUBLIC }
    val expectedMemberFunctions = expected.memberFunctions.filter { it.visibility == KVisibility.PUBLIC }
    expectedMemberFunctions.forEach { if (!actualMemberFunctions.contains(it)) error("Public function ${it} not present on $this") }

    val actualMemberProperties = this.memberProperties.filter { it.visibility == KVisibility.PUBLIC }
    val expectedMemberProperties = expected.memberProperties.filter { it.visibility == KVisibility.PUBLIC }
    expectedMemberProperties.forEach { if (!actualMemberProperties.contains(it)) error("Public property ${it} not present on $this") }
}
