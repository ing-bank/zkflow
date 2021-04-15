package zinc.types

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serializers.BFLSerializers
import com.ing.serialization.bfl.serializers.CurrencySerializer
import com.ing.zknotary.common.serialization.bfl.corda.AmountSerializer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.Amount
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalSerializationApi
class SerializedAmountMaxTest {
    private val log = loggerFor<SerializedAmountMaxTest>()
    private val zincZKService = getZincZKService<SerializedAmountMaxTest>()

    @BeforeAll
    fun setup() {
        zincZKService.setup()
    }

    @AfterAll
    fun cleanup() {
        zincZKService.cleanup()
    }

    @Test
    @ExperimentalUnsignedTypes
    fun `get max amount`() {
        val expectedAmount = Amount(250, canadianCurrency)
        val expected = expectedAmount.toJSON(
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
        val witnessBytes: ByteArray = serialize(witnessData, serializersModule = BFLSerializers + currencySerializersModule)

        verifyZincRunResults(witnessBytes, expected, expectedAmount)

        zincZKService.proveTimed(witnessBytes.toJsonWitness(), log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @ExperimentalUnsignedTypes
    private fun verifyZincRunResults(
        witnessBytes: ByteArray,
        expected: String,
        expectedAmount: Amount<Currency>
    ) {
        val actual = zincZKService.run(witnessBytes.toJsonWitness(), "")

        val actualElement = Json.parseToJsonElement(actual)
        val expectedElement = Json.parseToJsonElement(expected)

        actualElement.jsonObject["quantity"] shouldNotBe null
        actualElement.jsonObject["quantity"] shouldBe expectedElement.jsonObject["quantity"]
        actualElement.jsonObject["display_token_size"] shouldNotBe null
        actualElement.jsonObject["display_token_size"] shouldBe expectedElement.jsonObject["display_token_size"]
        actualElement.jsonObject["token_type_hash"] shouldNotBe null
        actualElement.jsonObject["token_type_hash"] shouldBe Json.parseToJsonElement(
            expectedAmount.token.javaClass.sha256().toPrettyJSONArray()
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

        @ExperimentalUnsignedTypes
        private fun ByteArray.toJsonWitness(): String {
            return "{\"witness\": [${this.joinToString(",") { "\"${it.toUByte()}\"" }}] }"
        }
    }
}
