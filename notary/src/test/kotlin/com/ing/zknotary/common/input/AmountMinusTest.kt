package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZKProvingException
import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class AmountMinusTest {
    companion object {
        private val circuitFolder: String = AmountMinusTest::class.java.getResource("/AmountMinusTest").path
        private val sharedModules: Array<String> = arrayOf(AmountMinusTest::class.java.getResource("/shared/floating_point.zn").path.toString())
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
            composeTempCircuit(circuitFolder, sharedModules)
            zincZKService.setup()
        }

        @AfterAll
        @JvmStatic
        fun `remove zinc files`() {
            zincZKService.cleanup()
        }
    }

    @Test
    fun `zinc plus fails due to different token sizes`() {
        val exception = Assertions.assertThrows(ZKProvingException::class.java) {
            // 10
            val leftSign: Byte = 1
            val leftInteger = ByteArray(1024)
            leftInteger[1022] = 1
            val leftFraction = ByteArray(128)
            val leftDisplayTokenSize = toBigDecimalJSON(leftSign, leftInteger, leftFraction)

            val leftQuantity: Long = 100
            val leftTokenHash = ByteArray(128)
            val leftAmount = toAmountJSON(leftQuantity, leftDisplayTokenSize, leftTokenHash)

            // 1
            val rightSign: Byte = 1
            val rightInteger = ByteArray(1024)
            rightInteger[1023] = 1
            val rightFraction = ByteArray(128)
            val rightDisplayTokenSize = toBigDecimalJSON(rightSign, rightInteger, rightFraction)

            val rightQuantity: Long = 100
            val rightTokenHash = ByteArray(128)
            val rightAmount = toAmountJSON(rightQuantity, rightDisplayTokenSize, rightTokenHash)

            val input = "{\"left\": $leftAmount" +
                ",\"right\": $rightAmount}"

            zincZKService.prove(input.toByteArray())
        }

        Assertions.assertTrue(
            exception.message?.contains("Token sizes don't match") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc plus fails due to different token hashes`() {
        val exception = Assertions.assertThrows(ZKProvingException::class.java) {
            // 1
            val leftSign: Byte = 1
            val leftInteger = ByteArray(1024)
            leftInteger[1023] = 1
            val leftFraction = ByteArray(128)
            val leftDisplayTokenSize = toBigDecimalJSON(leftSign, leftInteger, leftFraction)

            val leftQuantity: Long = 100
            val leftTokenHash = ByteArray(128)
            leftTokenHash[127] = 1
            val leftAmount = toAmountJSON(leftQuantity, leftDisplayTokenSize, leftTokenHash)

            // 1
            val rightSign: Byte = 1
            val rightInteger = ByteArray(1024)
            rightInteger[1023] = 1
            val rightFraction = ByteArray(128)
            val rightDisplayTokenSize = toBigDecimalJSON(rightSign, rightInteger, rightFraction)

            val rightQuantity: Long = 100
            val rightTokenHash = ByteArray(128)
            val rightAmount = toAmountJSON(rightQuantity, rightDisplayTokenSize, rightTokenHash)

            val input = "{\"left\": $leftAmount" +
                ",\"right\": $rightAmount}"

            zincZKService.prove(input.toByteArray())
        }

        Assertions.assertTrue(
            exception.message?.contains("Tokens don't match") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc plus smoke test`() {
        // 1
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        val leftDisplayTokenSize = toBigDecimalJSON(leftSign, leftInteger, leftFraction)

        val leftQuantity: Long = 200
        val leftTokenHash = ByteArray(128)
        val leftAmount = toAmountJSON(leftQuantity, leftDisplayTokenSize, leftTokenHash)

        // 1
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)
        val rightDisplayTokenSize = toBigDecimalJSON(rightSign, rightInteger, rightFraction)

        val rightQuantity: Long = 100
        val rightTokenHash = ByteArray(128)
        val rightAmount = toAmountJSON(rightQuantity, rightDisplayTokenSize, rightTokenHash)

        val input = "{\"left\": $leftAmount" +
            ",\"right\": $rightAmount}"

        val sign: Byte = 1
        val integer = ByteArray(1024)
        integer[1023] = 1
        val fraction = ByteArray(128)
        val displayTokenSize = toBigDecimalJSON(sign, integer, fraction)

        val quantity: Long = 100
        val tokenHash = ByteArray(128)
        val amount = toAmountJSON(quantity, displayTokenSize, tokenHash)

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, amount.toByteArray())
    }
}
