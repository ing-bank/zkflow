package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serializers.CurrencySerializer
import com.ing.zknotary.common.serialization.roundTrip
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.contracts.TokenizableAssetInfo
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.OpaqueBytes
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

internal class AmountSerializerTest {
    @Test
    fun `deserialized Amount with CustomData should equal original`() {
        val original = DataWithCustomData(Amount(5L, BigDecimal.TEN, CustomData("Hello BFL!")))
        roundTrip(original, serializers = SerializersModule { contextual(AmountSerializer(CustomData.serializer())) })
    }

    @Test
    fun `deserialized Amount with String should equal original`() {
        val original = DataWithString(Amount(5L, BigDecimal.ONE, "Hello BFL!"))
        roundTrip(original, serializers = SerializersModule { contextual(AmountSerializer(String.serializer())) })
    }

    @Test
    fun `deserialized Amount with Currency should equal original`() {
        val original = DataWithCurrency(Amount(5L, BigDecimal.ONE, Currency.getInstance(Locale.CANADA)))
        roundTrip(original, serializers = SerializersModule { contextual(AmountSerializer(CurrencySerializer)) })
    }

    @Test
    fun `deserialized Amount with Issued should equal original`() {
        val issued = Issued(
            PartyAndReference(TestIdentity.fresh("test").party, OpaqueBytes("world".toByteArray())),
            42
        )
        val original = DataWithIssued(Amount(5L, BigDecimal.ONE, issued))
        roundTrip(
            original,
            serializers = CordaSerializers +
                CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.DEFAULT_SIGNATURE_SCHEME) +
                SerializersModule { contextual(AmountSerializer(IssuedSerializer(Int.serializer()))) }
        )
    }

    @Test
    fun `deserialized Amount with TokenizableAssetInfo should equal original`() {
        val original = DataWithTokenizableAssetInfo(
            Amount(5L, BigDecimal.ONE, MyTokenizableAssetInfo(BigDecimal.TEN))
        )
        roundTrip(
            original,
            serializers = SerializersModule { contextual(AmountSerializer(MyTokenizableAssetInfo.serializer())) }
        )
    }
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
private data class DataWithIssued(
    val amount: @Contextual Amount<@Contextual Issued<@Contextual Any>>
)

@Serializable
private data class MyTokenizableAssetInfo(
    @FixedLength([AmountSurrogate.DISPLAY_TOKEN_SIZE_INTEGER_LENGTH, AmountSurrogate.DISPLAY_TOKEN_SIZE_FRACTION_LENGTH])
    override val displayTokenSize: @Contextual BigDecimal
) : TokenizableAssetInfo

@Serializable
private data class DataWithTokenizableAssetInfo(
    val amount: @Contextual Amount<MyTokenizableAssetInfo>
)

@Serializable
private data class DataWithString(
    @FixedLength([32])
    val amount: @Contextual Amount<String>
)
