package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test

class ClaimSerializerTest {
    @Serializable
    data class Data(
        @Serializable(with = ClaimSerializer::class)
        @FixedLength([7])
        val stringClaim: @Contextual Claim<String>,
        @Serializable(with = ClaimSerializer::class)
        val intClaim: @Contextual Claim<Int>

    )

    private val claimWithString1 : Claim<String> = Claim("Property 1", "Value 1")
    private val claimWithString2 : Claim<String> = Claim("Property 2", "Value 2")

    private val claimWithInt1 : Claim<Int> = Claim("Property 1", 1)
    private val claimWithInt2 : Claim<Int> = Claim("Property 2", 2)

    @Test
    fun `serialize and deserialize Claim with String directly`() {
        assertRoundTripSucceeds(claimWithString1, strategy = ClaimSerializer(String.serializer()), outerFixedLength = intArrayOf(7))
        assertSameSize(claimWithString1, claimWithString2, strategy = ClaimSerializer(String.serializer()), outerFixedLength = intArrayOf(7))
    }

    @Test
    fun `serialize and deserialize Claim with Int directly`() {
        assertRoundTripSucceeds(claimWithInt1, strategy = ClaimSerializer(Int.serializer()))
        assertSameSize(claimWithInt1, claimWithInt2, strategy = ClaimSerializer(Int.serializer()))
    }

    @Test
    fun `serialize and deserialize Claim`() {
        val data1 = Data(claimWithString1, claimWithInt1)
        val data2 = Data(claimWithString2, claimWithInt2)

        assertRoundTripSucceeds(data1)
        assertSameSize(data1, data2)
    }
}