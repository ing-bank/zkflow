package com.ing.zknotary.common.zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration

class AmountEqualsTest {
    private val circuitFolder: String = AmountEqualsTest::class.java.getResource("/AmountEqualsTest").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private val dummyToken = "com.ing.zknotary.SuperToken"
    private val anotherDummyToken = 420

    init {
        zincZKService.setup()
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc equals with different quantities`() {
        val left = Amount(200, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("1"), dummyToken)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc equals with different display token sizes`() {
        val left = Amount(200, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("2"), dummyToken)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc equals with different token hashes`() {
        val left = Amount(200, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("1"), anotherDummyToken)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc smoke test`() {
        val left = Amount(100, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("1"), dummyToken)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }
}
