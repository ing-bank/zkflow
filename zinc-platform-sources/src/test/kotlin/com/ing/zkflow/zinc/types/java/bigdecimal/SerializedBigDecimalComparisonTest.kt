package com.ing.zkflow.zinc.types.java.bigdecimal

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.proveTimed
import com.ing.zkflow.zinc.types.setupTimed
import com.ing.zkflow.zinc.types.toSerializedWitness
import com.ing.zkflow.zinc.types.verifyTimed
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class SerializedBigDecimalComparisonTest {
    private val log = loggerFor<SerializedBigDecimalComparisonTest>()
    private val zincZKService = getZincZKService<SerializedBigDecimalComparisonTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc compares two positives (left sign is greater)`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal("-1.1")

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (right sign is greater)`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal("1.1")

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (left integer is greater)`() {
        val left = BigDecimal.ONE
        val right = BigDecimal.ZERO

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (right integer is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal.ONE

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (left fraction is greater)`() {
        val left = BigDecimal("0.1")
        val right = BigDecimal.ZERO

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (right fraction is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("0.1")

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two numbers of the same digits, but in opposite order (smoke test for big-endianness)`() {
        val left = BigDecimal("123.123")
        val right = BigDecimal("321.321")

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two numbers of different sizes`() {
        val left = BigDecimal("12")
        val right = BigDecimal("5")

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two numbers of the same digits, but in opposite order reverse (smoke test for big-endianness)`() {
        val left = BigDecimal("321")
        val right = BigDecimal("123")

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two zeros`() {
        val zero = BigDecimal.ZERO

        val input = toSerializedWitness(zero, zero)
        val expected = "\"${zero.compareTo(zero)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toSerializedWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }
}
