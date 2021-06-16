package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.Test

class AbstractClaimSerializerTest {
    @Serializable
    data class ClaimDataWithString(@FixedLength([7]) val value: @Polymorphic AbstractClaim<String>)

    @Serializable
    data class ClaimDataWithInt(val value: @Polymorphic AbstractClaim<Int>)

    private val claimWithString1 : AbstractClaim<String> = Claim("Property 1", "Value 1")
    private val claimWithString2 : AbstractClaim<String> = Claim("Property 2", "Value 2")

    private val claimWithInt1 : AbstractClaim<Int> = Claim("Property 1", 1)
    private val claimWithInt2 : AbstractClaim<Int> = Claim("Property 2", 2)

    private val serializersModule = SerializersModule {
        // we need to polymorphically register the inner possible implementation classes of a generic as subtypes of Any
        polymorphic(Any::class) {
            subclass(Int::class, MyIntSerializer)
            subclass(String::class, MyStringSerializer)
        }

        // we need to provide the inner serializer of an abstract class with generic as a PolymorphicSerializer instance
        // with Any class as its base (according to kotlinx docs)
        polymorphic(AbstractClaim::class) {
            subclass(ClaimSerializer(PolymorphicSerializer(Any::class)))
        }
    }

    @Test
    fun `serialize and deserialize AbstractClaim with String`() {
        val data1 = ClaimDataWithString(claimWithString1)
        val data2 = ClaimDataWithString(claimWithString2)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }

    @Test
    fun `serialize and deserialize AbstractClaim with Int`() {
        val data1 = ClaimDataWithInt(claimWithInt1)
        val data2 = ClaimDataWithInt(claimWithInt2)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}