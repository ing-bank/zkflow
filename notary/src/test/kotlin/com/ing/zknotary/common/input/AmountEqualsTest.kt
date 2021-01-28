package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class AmountEqualsTest {
    companion object {
        private val circuitFolder: String = AmountEqualsTest::class.java.getResource("/AmountEqualsTest").path
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
    fun `zinc equals with different quantities`() {
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        val leftDisplayTokenSize = toBigDecimalJSON(leftSign, leftInteger, leftFraction)

        val leftQuantity: Long = 200
        val leftTokenHash = ByteArray(128)
        val leftAmount = toAmountJSON(leftQuantity, leftDisplayTokenSize, leftTokenHash)

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

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc equals with different display token sizes`() {
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1022] = 1
        val leftFraction = ByteArray(128)
        val leftDisplayTokenSize = toBigDecimalJSON(leftSign, leftInteger, leftFraction)

        val leftQuantity: Long = 100
        val leftTokenHash = ByteArray(128)
        val leftAmount = toAmountJSON(leftQuantity, leftDisplayTokenSize, leftTokenHash)

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

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc equals with different token hashes`() {
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        val leftDisplayTokenSize = toBigDecimalJSON(leftSign, leftInteger, leftFraction)

        val leftQuantity: Long = 100
        val leftTokenHash = ByteArray(128)
        leftTokenHash[127] = 1
        val leftAmount = toAmountJSON(leftQuantity, leftDisplayTokenSize, leftTokenHash)

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

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc smoke test`() {
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        val leftDisplayTokenSize = toBigDecimalJSON(leftSign, leftInteger, leftFraction)

        val leftQuantity: Long = 100
        val leftTokenHash = ByteArray(128)
        val leftAmount = toAmountJSON(leftQuantity, leftDisplayTokenSize, leftTokenHash)

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

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"0\"".toByteArray())
    }
}
