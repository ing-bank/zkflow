package com.ing.zkflow.zinc.types.corda.amount

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serializers.BFLSerializers
import com.ing.serialization.bfl.serializers.CurrencySerializer
import com.ing.zkflow.common.zkp.ZKRunException
import com.ing.zkflow.serialization.bfl.corda.AmountSerializer
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.testing.toJsonArray
import com.ing.zkflow.zinc.types.sha256
import com.ing.zkflow.zinc.types.toZincJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Currency
import java.util.Locale

class SerializedAmountMaxTest {
    private val zincZKService = getZincZKService<SerializedAmountMaxTest>()

    init {
        zincZKService.setup()
    }

    @AfterAll
    fun cleanup() {
        zincZKService.cleanup()
    }

    @Test
    @ExperimentalUnsignedTypes
    fun `get max amount`() {
        val expectedAmount = Amount(250, frenchCurrency)
        verifyMaxAmountTest(expectedAmount)
    }

    @Test
    @ExperimentalUnsignedTypes
    fun `get max amount should fail for incompatible tokens`() {
        val expectedAmount = Amount(250, canadianCurrency)
        assertThrows<ZKRunException> {
            verifyMaxAmountTest(expectedAmount)
        }.also {
            assertTrue(
                it.message?.contains("Tokens don't match") ?: false,
                "Circuit fails with different error"
            )
        }
    }

    private fun verifyMaxAmountTest(expectedAmount: Amount<Currency>) {
        val expected = expectedAmount.toZincJson(
            integerSize = 100,
            fractionSize = 20
        )

        val witnessData = Data(
            listOf(
                Amount(100, frenchCurrency),
                expectedAmount,
                Amount(220, frenchCurrency),
            )
        )
        val witnessBytes: ByteArray =
            serialize(witnessData, serializersModule = BFLSerializers + currencySerializersModule)

        verifyZincRunResults(witnessBytes, expected, expectedAmount)

        zincZKService.proveTimed(witnessBytes.toJsonWitness()).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @ExperimentalUnsignedTypes
    private fun verifyZincRunResults(
        witnessBytes: ByteArray,
        expected: String,
        expectedAmount: Amount<Currency>
    ) {
        val actual = zincZKService.run(witnessBytes.toJsonWitness())

        val actualElement = Json.parseToJsonElement(actual)
        val expectedElement = Json.parseToJsonElement(expected)

        actualElement.jsonObject["quantity"] shouldNotBe null
        actualElement.jsonObject["quantity"] shouldBe expectedElement.jsonObject["quantity"]
        actualElement.jsonObject["display_token_size"] shouldNotBe null
        actualElement.jsonObject["display_token_size"] shouldBe expectedElement.jsonObject["display_token_size"]
        actualElement.jsonObject["token_type_hash"] shouldNotBe null
        actualElement.jsonObject["token_type_hash"] shouldBe Json.parseToJsonElement(
            expectedAmount.token.javaClass.sha256().toJsonArray().toString()
        )
        actualElement.jsonObject["token"] shouldNotBe null
        actualElement.jsonObject["token"] shouldBe expectedElement.jsonObject["token"]
    }

    companion object {
        val canadianCurrency: Currency = Currency.getInstance(Locale.CANADA)
        val frenchCurrency: Currency = Currency.getInstance(Locale.FRANCE)
        @Serializable
        data class Data(
            @FixedLength([4]) val list: List<@Contextual Amount<@Contextual Currency>>
        ) {
            init {
                if (list.size > 4) {
                    throw IllegalArgumentException("${this.javaClass.simpleName}.list.size should not exceed 4")
                }
            }
        }

        private val currencySerializersModule = SerializersModule {
            contextual(AmountSerializer(CurrencySerializer))
        }

        private fun ByteArray.toJsonWitness() = buildJsonObject {
            put("witness", toJsonArray())
        }.toString()
    }
}
