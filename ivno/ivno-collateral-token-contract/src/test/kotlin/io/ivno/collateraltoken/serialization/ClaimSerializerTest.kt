package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.serialization.bfl.serializers.StateRefSerializer
import com.ing.zkflow.testing.assertRoundTripSucceeds
import com.ing.zkflow.testing.assertSameSize
import io.ivno.collateraltoken.zinc.types.anotherClaimWithContextual
import io.ivno.collateraltoken.zinc.types.anotherClaimWithInt
import io.ivno.collateraltoken.zinc.types.anotherClaimWithPolymorphic
import io.ivno.collateraltoken.zinc.types.anotherClaimWithString
import io.ivno.collateraltoken.zinc.types.claimWithContextual
import io.ivno.collateraltoken.zinc.types.claimWithInt
import io.ivno.collateraltoken.zinc.types.claimWithPolymorphic
import io.ivno.collateraltoken.zinc.types.claimWithString
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty
import org.junit.jupiter.api.Test

class ClaimSerializerTest {
    @Serializable
    data class Data(
        @Serializable(with = ClaimSerializer::class)
        @FixedLength([7])
        val stringClaim: Claim<String>,
        @Serializable(with = ClaimSerializer::class)
        val intClaim: Claim<Int>,
        @Serializable(with = ClaimSerializer::class)
        val contextualClaim: Claim<@Contextual StateRef>,
        @Serializable(with = ClaimSerializer::class)
        val polymorphicClaim: Claim<@Polymorphic AbstractParty>,
    )

    @Test
    fun `serialize and deserialize Claim with String directly`() {
        assertRoundTripSucceeds(claimWithString, strategy = ClaimSerializer(String.serializer()), outerFixedLength = intArrayOf(7))
        assertSameSize(claimWithString, anotherClaimWithString, strategy = ClaimSerializer(String.serializer()), outerFixedLength = intArrayOf(7))
    }

    @Test
    fun `serialize and deserialize Claim with Int directly`() {
        assertRoundTripSucceeds(claimWithInt, strategy = ClaimSerializer(Int.serializer()))
        assertSameSize(claimWithInt, anotherClaimWithInt, strategy = ClaimSerializer(Int.serializer()))
    }

    @Test
    fun `serialize and deserialize Claim with Contextual directly`() {
        assertRoundTripSucceeds(claimWithContextual, strategy = ClaimSerializer(StateRefSerializer))
        assertSameSize(claimWithContextual, anotherClaimWithContextual, strategy = ClaimSerializer(StateRefSerializer))
    }

    @Test
    fun `serialize and deserialize Claim with Polymorphic directly`() {
        assertRoundTripSucceeds(claimWithPolymorphic, strategy = ClaimSerializer(PolymorphicSerializer(AbstractParty::class)))
        assertSameSize(claimWithPolymorphic, anotherClaimWithPolymorphic, strategy = ClaimSerializer(PolymorphicSerializer(AbstractParty::class)))
    }

    @Test
    fun `serialize and deserialize Claim`() {
        val data1 = Data(claimWithString, claimWithInt, claimWithContextual, claimWithPolymorphic)
        val data2 = Data(anotherClaimWithString, anotherClaimWithInt, anotherClaimWithContextual, anotherClaimWithPolymorphic)

        assertRoundTripSucceeds(data1)
        assertSameSize(data1, data2)
    }
}