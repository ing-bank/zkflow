package zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BigBigDecimalMinusTest {
    private val log = loggerFor<BigDecimalMinusTest>()

    private val circuitFolder: String = BigDecimalMinusTest::class.java.getResource("/BigBigDecimalMinusTest").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(1800),
        provingTimeout = Duration.ofSeconds(1800),
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
    fun `BigDecimal compatibility - zinc subtraction of two of the same positive scale`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", 10)
        val right = makeBigDecimal("747233429293018787918347987234564568", 10)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two of the same negative scale`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", -10)
        val right = makeBigDecimal("747233429293018787918347987234564568", -10)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two with different scales (left - positive, right - negative)`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", 15)
        val right = makeBigDecimal("747233429293018787918347987234564568", -10)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two with different scales (left - negative, right -  positive)`() {
        val left = makeBigDecimal("0", -15)
        val right = makeBigDecimal("0", 10)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of different length (left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of different length (right is longer)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different length (left - positive, right - negative, left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of the same length (left - positive, right - negative, right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers (left - negative, right - positive, left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negative numbers (left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negative numbers (right is longer)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)

        val input = toBigWitness(left, right)
        val expected = left.minus(right).toJSON(100, 20)

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }
}