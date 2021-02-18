package com.ing.zknotary.common.zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Tag("nightly")
class BigDecimalEqualsTest {
    private val log = loggerFor<BigDecimalEqualsTest>()

    private val circuitFolder: String = BigDecimalEqualsTest::class.java.getResource("/BigDecimalEqualsTest").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc compares using equals (sign is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("-421.42")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares using equals (integer is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("42.42")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares using equals (fraction is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.421")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares using equals (both are equal to each other)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.42")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of unequals`() {
        val left = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)
        val right = makeBigDecimal("7472334223847623782375469293018787918347987234564568", 13)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of unequals 2`() {
        val left = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)
        val right = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", 13)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of unequals 3`() {
        val left = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)
        val right = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", 0)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of equals`() {
        val left = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)
        val right = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc equal of equals`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc equal of unequals`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }
}
