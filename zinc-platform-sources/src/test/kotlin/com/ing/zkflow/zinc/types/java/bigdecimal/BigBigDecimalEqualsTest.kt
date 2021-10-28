package com.ing.zkflow.zinc.types.java.bigdecimal

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.testing.zkp.proveTimed
import com.ing.zkflow.testing.zkp.setupTimed
import com.ing.zkflow.testing.zkp.verifyTimed
import com.ing.zkflow.zinc.types.makeBigDecimal
import com.ing.zkflow.zinc.types.toBigWitness
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class BigBigDecimalEqualsTest {
    private val zincZKService = getZincZKService<BigBigDecimalEqualsTest>()

    init {
        zincZKService.setupTimed()
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `BigInteger compatibility - zinc equal of unequals`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toBigWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc equal of equals`() {
        val left = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)
        val right = makeBigDecimal(byteArrayOf(12, 56, 100, -2, -76, 89, 45, 91, 3, -15, 35, 26, 3, 91), 1)

        val input = toBigWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of unequals`() {
        val left = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)
        val right = makeBigDecimal("7472334223847623782375469293018787918347987234564568", 13)

        val input = toBigWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of unequals 2`() {
        val left = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)
        val right = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", 13)

        val input = toBigWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of unequals 3`() {
        val left = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)
        val right = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", 0)

        val input = toBigWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc equal of equals`() {
        val left = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)
        val right = makeBigDecimal("92948782094488478231212478987482988429808779810457634781384756794987", -24)

        val input = toBigWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }
}