package io.ivno.collateraltoken.serialization

import com.ing.zkflow.common.serialization.bfl.serializers.AnonymousPartySerializer
import com.ing.zkflow.common.serialization.bfl.serializers.PartySerializer
import com.ing.zkflow.common.serialization.bfl.serializers.StateRefSerializer
import com.ing.zkflow.testing.assertRoundTripSucceeds
import com.ing.zkflow.testing.assertSameSize
import io.ivno.collateraltoken.zinc.types.abstractClaimWithContextual
import io.ivno.collateraltoken.zinc.types.abstractClaimWithInt
import io.ivno.collateraltoken.zinc.types.abstractClaimWithPolymorphic
import io.ivno.collateraltoken.zinc.types.abstractClaimWithString
import io.ivno.collateraltoken.zinc.types.anotherAbstractClaimWithContextual
import io.ivno.collateraltoken.zinc.types.anotherAbstractClaimWithInt
import io.ivno.collateraltoken.zinc.types.anotherAbstractClaimWithPolymorphic
import io.ivno.collateraltoken.zinc.types.anotherAbstractClaimWithString
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import org.junit.jupiter.api.Test

class AbstractClaimSerializerTest {
    @Serializable
    data class ClaimData(
        val stringClaim: @Polymorphic AbstractClaim<String>,
        val intClaim: @Polymorphic AbstractClaim<Int>,
        val contextualClaim: @Polymorphic AbstractClaim<@Contextual StateRef>,
        val polymorphicClaim: @Polymorphic AbstractClaim<@Polymorphic AbstractParty>,
    )

    private val serializersModule = SerializersModule {
        // special handling needed for registering multiple superclasses for Party and AnonymousParty
        fun PolymorphicModuleBuilder<AbstractParty>.registerAbstractPartySubclasses() {
            subclass(Party::class, PartySerializer)
            subclass(AnonymousParty::class, AnonymousPartySerializer)
        }

        // we need to polymorphically register the inner possible implementation classes of a generic as subtypes of Any
        polymorphic(Any::class) {
            subclass(Int::class, MyIntSerializer)
            subclass(String::class, MyStringSerializer)
            subclass(StateRef::class, StateRefSerializer)
            registerAbstractPartySubclasses()
        }

        // we need to provide the inner serializer of an abstract class with generic as a PolymorphicSerializer instance
        // with Any class as its base (according to kotlinx docs)
        polymorphic(AbstractClaim::class) {
            subclass(ClaimSerializer(PolymorphicSerializer(Any::class)))
        }
    }

    @Test
    fun `serialize and deserialize AbstractClaim`() {
        val data1 = ClaimData(abstractClaimWithString, abstractClaimWithInt, abstractClaimWithContextual, abstractClaimWithPolymorphic)
        val data2 = ClaimData(anotherAbstractClaimWithString, anotherAbstractClaimWithInt, anotherAbstractClaimWithContextual, anotherAbstractClaimWithPolymorphic)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}