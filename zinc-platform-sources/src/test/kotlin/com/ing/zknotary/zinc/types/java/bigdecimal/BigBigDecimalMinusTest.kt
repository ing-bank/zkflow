package com.ing.zknotary.zinc.types.java.bigdecimal

import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.makeBigDecimal
import com.ing.zknotary.zinc.types.proveTimed
import com.ing.zknotary.zinc.types.setupTimed
import com.ing.zknotary.zinc.types.toBigWitness
import com.ing.zknotary.zinc.types.toZincJson
import com.ing.zknotary.zinc.types.verifyTimed
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BigBigDecimalMinusTest {
    private val log = loggerFor<BigBigDecimalMinusTest>()
    private val zincZKService = getZincZKService<BigBigDecimalMinusTest>(
        setupTimeout = Duration.ofSeconds(1800),
        provingTimeout = Duration.ofSeconds(1800),
    )

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two of the same positive scale`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", 10)
        val right = makeBigDecimal("747233429293018787918347987234564568", 10)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two of the same negative scale`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", -10)
        val right = makeBigDecimal("747233429293018787918347987234564568", -10)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two with different scales (left - positive, right - negative)`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", 15)
        val right = makeBigDecimal("747233429293018787918347987234564568", -10)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two with different scales (left - negative, right -  positive)`() {
        val left = makeBigDecimal("0", -15)
        val right = makeBigDecimal("0", 10)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of different length (left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of different length (right is longer)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different length (left - positive, right - negative, left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of the same length (left - positive, right - negative, right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers (left - negative, right - positive, left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negative numbers (left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negative numbers (right is longer)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }
}
