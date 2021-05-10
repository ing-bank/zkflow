package zinc.types

import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BigBigDecimalPlusTest {
    private val log = loggerFor<BigBigDecimalPlusTest>()
    private val zincZKService = getZincZKService<BigBigDecimalPlusTest>(
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
    fun `BigDecimal compatibility - zinc sum of two of the same positive scale`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", 10)
        val right = makeBigDecimal("747233429293018787918347987234564568", 10)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc sum of two of the same negative scale`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", -10)
        val right = makeBigDecimal("747233429293018787918347987234564568", -10)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc sum of two with different scales (first - positive, second negative)`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", 15)
        val right = makeBigDecimal("747233429293018787918347987234564568", -10)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc sum of two with different scales (left - negative, right - positive)`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", -15)
        val right = makeBigDecimal("747233429293018787918347987234564568", 10)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc sum of two zeroes with different scales (left - negative, right - positive)`() {
        val left = makeBigDecimal("0", -15)
        val right = makeBigDecimal("0", 10)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two positives of different length (left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two positives of different length (right is longer)`() {
        val left = BigDecimal(BigInteger(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)))
        val right = BigDecimal(BigInteger(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)))

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two negatives of different length (left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two negatives of different length (right is longer)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of different length (left - positive, right - negative, left - longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of different length (left - positive, right - negative, right - longer)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of different length (left - negative, right - positive, left - longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of different length (left - negative, right - positive, right - longer)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers so that carry is 1`() {
        val left = makeBigDecimal(byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1), 1)
        val right = makeBigDecimal(byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1), 1)

        val input = toBigWitness(left, right)
        val expected = left.plus(right).toZincJson(100, 20)

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }
}
