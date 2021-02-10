package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

class BigDecimalEqualsTest {
    companion object {
        private val circuitFolder: String = BigDecimalEqualsTest::class.java.getResource("/BigDecimalEqualsTest").path
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
    fun `zinc compares using equals (sign is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("-421.42")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares using equals (integer is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("42.42")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares using equals (fraction is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.421")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares using equals (both are equal to each other)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.42")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of unequals`() {
        val leftString = "92948782094488478231212478987482988429808779810457634781384756794987"
        val leftScale = -24
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "7472334223847623782375469293018787918347987234564568"
        val rightScale = 13
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of unequals 2`() {
        val leftString = "92948782094488478231212478987482988429808779810457634781384756794987"
        val leftScale = -24
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "92948782094488478231212478987482988429808779810457634781384756794987"
        val rightScale = 13
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of unequals 3`() {
        val leftString = "92948782094488478231212478987482988429808779810457634781384756794987"
        val leftScale = -24
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "92948782094488478231212478987482988429808779810457634781384756794987"
        val right = BigDecimal(BigInteger(rightString))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of equals`() {
        val leftString = "92948782094488478231212478987482988429808779810457634781384756794987"
        val leftScale = -24
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "92948782094488478231212478987482988429808779810457634781384756794987"
        val rightScale = -24
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc equal of equals`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc equal of unequals`() {
        val leftBytes = byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(45, 91, 3, -15, 35, 26, 3, 91)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }
}
