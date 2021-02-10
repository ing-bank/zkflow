package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

class BigDecimalComparisonTest {
    companion object {
        private val circuitFolder: String = BigDecimalComparisonTest::class.java.getResource("/BigDecimalComparisonTest").path
        private val zincZKService = ZincZKService(
            circuitFolder,
            artifactFolder = circuitFolder,
            buildTimeout = Duration.ofSeconds(5),
            setupTimeout = Duration.ofSeconds(300),
            provingTimeout = Duration.ofSeconds(300),
            verificationTimeout = Duration.ofSeconds(1)
        )

        @BeforeAll
        @JvmStatic
        fun setup() {
            zincZKService.setup()
        }

        @AfterAll
        @JvmStatic
        fun `remove zinc files`() {
            zincZKService.cleanup()
        }
    }

    @Test
    fun `zinc compares two positives (left sign is greater)`() {
        val left = BigDecimal(1.1)
        val right = BigDecimal(-1.1)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (right sign is greater)`() {
        val left = BigDecimal(-1.1)
        val right = BigDecimal(1.1)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (left integer is greater)`() {
        val left = BigDecimal.ONE
        val right = BigDecimal.ZERO

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (right integer is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal.ONE

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (left fraction is greater)`() {
        val left = BigDecimal(0.1)
        val right = BigDecimal.ZERO

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (right fraction is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal(0.1)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two number of the same digits, but in opposite order (smoke test for big-endianness)`() {
        val left = BigDecimal("123")
        val right = BigDecimal("321")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two zeros`() {
        val zero = BigDecimal.ZERO

        val input = "{\"left\": ${zero.toJSON()}" +
            ",\"right\": ${zero.toJSON()}}"

        val expected = "\"${zero.compareTo(zero)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc compare the same scale`() {
        val leftString = "12380964839238475457356735674573563567890295784902768787678287"
        val leftScale = 18
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "4573563567890295784902768787678287"
        val rightScale = 18
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc compare the same scale 2`() {
        val leftString = "12380964839238475457356735674573563567890295784902768787678287"
        val leftScale = 18
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "4573563923487289357829759278282992758247567890295784902768787678287"
        val rightScale = 18
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater left scale`() {
        val leftString = "12380964839238475457356735674573563567890295784902768787678287"
        val leftScale = 28
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "4573563567890295784902768787678287"
        val rightScale = 18
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater left scale 2`() {
        val leftString = "12380964839238475457356735674573563567890295784902768787678287"
        val leftScale = 48
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "4573563567890295784902768787678287"
        val rightScale = 2
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater right scale`() {
        val leftString = "12380964839238475457356735674573563567890295784902768787678287"
        val leftScale = 18
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "4573563567890295784902768787678287"
        val rightScale = 28
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc compare with greater right scale 2`() {
        val leftString = "12380964839238475457356735674573"
        val leftScale = 36
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "45735635948573894578349572001798379183767890295784902768787678287"
        val rightScale = 48
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two positives with greater left`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two positives with greater right`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two equal positives`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two negatives (left abs value is greater)`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two negatives (right abs value is greater)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two equal negatives`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two numbers with different signs (left is positive)`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of two numbers with different signs (left is negative)`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of positive and ZERO`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val right = BigDecimal(BigInteger.ZERO)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and positive`() {
        val left = BigDecimal(BigInteger.ZERO)

        val rightBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of negative and ZERO`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val right = BigDecimal(BigInteger.ZERO)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and negative`() {
        val left = BigDecimal(BigInteger.ZERO)

        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = -1
        val right = BigDecimal(BigInteger(leftSign, leftBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    /**
     * compareTo(BigInteger a).
     * Compare ZERO to ZERO.
     */
    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }
}
