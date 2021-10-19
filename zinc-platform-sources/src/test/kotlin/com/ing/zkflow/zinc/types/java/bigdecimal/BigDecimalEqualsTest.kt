package com.ing.zkflow.zinc.types.java.bigdecimal

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.testing.zkp.proveTimed
import com.ing.zkflow.testing.zkp.setupTimed
import com.ing.zkflow.testing.zkp.verifyTimed
import com.ing.zkflow.zinc.types.toWitness
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BigDecimalEqualsTest {
    private val zincZKService = getZincZKService<BigDecimalEqualsTest>()

    init {
        zincZKService.setupTimed()
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc compares using equals (sign is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("-421.42")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc compares using equals (integer is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("42.42")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc compares using equals (fraction is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.421")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc compares using equals (both are equal to each other)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.42")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }
}
