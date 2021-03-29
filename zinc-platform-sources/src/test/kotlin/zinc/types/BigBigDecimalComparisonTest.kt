package zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BigBigDecimalComparisonTest {
    private val log = loggerFor<BigBigDecimalComparisonTest>()

    private val circuitFolder: String = BigBigDecimalComparisonTest::class.java.getResource("/BigBigDecimalComparisonTest").path
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
    fun `BigDecimal compatibility - zinc compare the same scale`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 18)
        val right = makeBigDecimal("4573563567890295784902768787678287", 18)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare the same scale 2`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 18)
        val right = makeBigDecimal("4573563923487289357829759278282992758247567890295784902768787678287", 18)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater left scale`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 20)
        val right = makeBigDecimal("4573563567890295784902768787678287", 10)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater left scale 2`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 20)
        val right = makeBigDecimal("4573563567890295784902768787678287", 0)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater right scale`() {
        val left = makeBigDecimal("12380964839238475457356735674573563567890295784902768787678287", 10)
        val right = makeBigDecimal("4573563567890295784902768787678287", 20)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater right scale 2`() {
        val left = makeBigDecimal("12380964839238475457356735674573", 0)
        val right = makeBigDecimal("45735635948573894578349572001798379183767890295784902768787678287", 20)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two positives with greater left`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two positives with greater right`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two equal positives`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two negatives (left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two negatives (right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two equal negatives`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two numbers with different signs (left is positive)`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two numbers with different signs (left is negative)`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of positive and ZERO`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and positive`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of negative and ZERO`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and negative`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), -1)

        val input = toBigWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }
}