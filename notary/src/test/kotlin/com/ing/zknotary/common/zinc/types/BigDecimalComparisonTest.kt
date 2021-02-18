package com.ing.zknotary.common.zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Tag("nightly")
class BigDecimalComparisonTest {
    private val log = loggerFor<BigDecimalComparisonTest>()

    private val circuitFolder: String = BigDecimalComparisonTest::class.java.getResource("/BigDecimalComparisonTest").path
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
    fun `zinc compares two positives (left sign is greater)`() {
        val left = BigDecimal(1.1)
        val right = BigDecimal(-1.1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (right sign is greater)`() {
        val left = BigDecimal(-1.1)
        val right = BigDecimal(1.1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (left integer is greater)`() {
        val left = BigDecimal.ONE
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (right integer is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal.ONE

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (left fraction is greater)`() {
        val left = BigDecimal(0.1)
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (right fraction is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal(0.1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two number of the same digits, but in opposite order (smoke test for big-endianness)`() {
        val left = BigDecimal("123")
        val right = BigDecimal("321")

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two zeros`() {
        val zero = BigDecimal.ZERO

        val input = "{\"left\": ${zero.toJSON()}, \"right\": ${zero.toJSON()}}"
        val expected = "\"${zero.compareTo(zero)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare the same scale`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 18)
        val right = makeBigDecimal("4573563567890295784902768787678287", 18)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare the same scale 2`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 18)
        val right = makeBigDecimal("4573563923487289357829759278282992758247567890295784902768787678287", 18)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater left scale`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 28)
        val right = makeBigDecimal("4573563567890295784902768787678287", 18)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater left scale 2`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 48)
        val right = makeBigDecimal("4573563567890295784902768787678287", 2)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater right scale`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 18)
        val right = makeBigDecimal("4573563567890295784902768787678287", 28)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater right scale 2`() {
        val left = makeBigDecimal("12380964839238475457356735674573", 36)
        val right = makeBigDecimal("45735635948573894578349572001798379183767890295784902768787678287", 48)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two positives with greater left`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two positives with greater right`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two equal positives`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two negatives (left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two negatives (right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two equal negatives`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two numbers with different signs (left is positive)`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two numbers with different signs (left is negative)`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of positive and ZERO`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and positive`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of negative and ZERO`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and negative`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }
}
