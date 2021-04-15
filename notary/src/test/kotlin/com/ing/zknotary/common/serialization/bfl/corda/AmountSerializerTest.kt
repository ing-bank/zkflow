package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serializers.CurrencySerializer
import com.ing.zknotary.common.serialization.roundTrip
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

internal class AmountSerializerTest {
    @Test
    fun `deserialized Amount with CustomData should equal original`() {
        val original = DataWithCustomData(Amount(5L, BigDecimal.TEN, CustomData("Hello BFL!")))
        roundTrip(original, serializers = customDataSerializersModule)
    }

    @Test
    fun `deserialized Amount with String should equal original`() {
        val original = DataWithString(Amount(5L, BigDecimal.ONE, "Hello BFL!"))
        roundTrip(original, serializers = stringSerializersModule)
    }

    @Test
    fun `deserialized Amount with Currency should equal original`() {
        val original = DataWithCurrency(Amount(5L, BigDecimal.ONE, Currency.getInstance(Locale.CANADA)))
        roundTrip(original, serializers = currencySerializersModule)
    }

    @Test
    fun `deserialized Amount with Currency2 should equal original`() {
        val original = DataWithCurrency2(Amount(5L, BigDecimal.ONE, Currency.getInstance(Locale.CANADA)))
        roundTrip(original, serializers = currencySerializersModule)
    }
}

private val currencySerializersModule = SerializersModule {
    // contextual(AmountStringSerializer)
    contextual(AmountSerializer(CurrencySerializer))
}

private val stringSerializersModule = SerializersModule {
    contextual(AmountSerializer(String.serializer()))
}

private val customDataSerializersModule = SerializersModule {
    contextual(AmountSerializer(CustomData.serializer()))
}

@Serializable
private data class CustomData(@FixedLength([32]) val data: String)

@Serializable
private data class DataWithCustomData(
    val amount: @Contextual Amount<CustomData>
)

@Serializable
private data class DataWithCurrency(
    val amount: @Contextual Amount<@Contextual Currency>
)

@Serializable
private data class DataWithCurrency2(
    val amount: @Contextual Amount<@Serializable(with = CurrencySerializer::class) Currency>
)

@Serializable
private data class DataWithString(
    @FixedLength([32])
    val amount: @Contextual Amount<String>
)
