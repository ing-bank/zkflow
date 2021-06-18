package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.Setting
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class MembershipSerializerTest {
    @Serializable
    data class DataWithInt(
        @Serializable(with = MembershipWithIntSerializer::class)
        val memberShip: Membership
    )

    @Serializable
    data class DataWithString(
        @Serializable(with = MembershipWithStringSerializer::class)
        @FixedLength([7])
        val memberShip: Membership
    )

    @Serializable
    data class DataWithIntAndString(
        @Serializable(with = MembershipWithIntAndStringSerializer::class)
        @FixedLength([7])
        val memberShip: Membership
    )

    private val serializersModule = SerializersModule {
        polymorphic(Any::class) {
            subclass(Int::class, MyIntSerializer)
            subclass(String::class, MyStringSerializer)
        }

        polymorphic(AbstractClaim::class) {
            subclass(ClaimSerializer(PolymorphicSerializer(Any::class)))
        }

        contextual(NetworkSerializer)
    }

    @ParameterizedTest
    @MethodSource("testMemberships")
    fun `serialize and deserialize Membership directly`(
        data1: Membership,
        data2: Membership,
        serializer: KSerializer<Any>,
        outerFixedLength: IntArray
    ) {
        assertRoundTripSucceeds(
            data1,
            serializers = serializersModule,
            strategy = serializer,
            outerFixedLength = outerFixedLength
        )
        assertSameSize(
            data1,
            data2,
            serializers = serializersModule,
            strategy = serializer,
            outerFixedLength = outerFixedLength
        )
    }

    @Test
    fun `serialize and deserialize Membership with Int`() {
        val data1 = DataWithInt(membershipWithInt)
        val data2 = DataWithInt(anotherMembershipWithInt)

        assertRoundTripSucceeds(data1, serializers = serializersModule)
        assertSameSize(data1, data2, serializers = serializersModule)
    }

    @Test
    fun `serialize and deserialize Membership with String`() {
        val data1 = DataWithString(membershipWithString)
        val data2 = DataWithString(anotherMembershipWithString)

        assertRoundTripSucceeds(data1, serializers = serializersModule)
        assertSameSize(data1, data2, serializers = serializersModule)
    }

    @Test
    fun `serialize and deserialize Membership with Int and String`() {
        val data1 = DataWithIntAndString(membershipWithIntAndString)
        val data2 = DataWithIntAndString(anotherMembershipWithIntAndString)

        assertRoundTripSucceeds(data1, serializers = serializersModule)
        assertSameSize(data1, data2, serializers = serializersModule)
    }

    companion object {
        private val alice = TestIdentity.fresh("Alice").party
        private val bob = TestIdentity.fresh("Bob").party
        private val network = Network(
            value = "Network",
            operator = alice
        )

        private val intClaimSet = setOf(Claim("Property 1", 1), Claim("Property 1", 2))
        private val stringClaimSet = setOf(Claim("Property 1", "Value 1"), Claim("Property 1", "Value 2"))

        private val intSettingsSet = setOf(Setting("Property 1", 1), Setting("Property 2", 2))
        private val stringSettingsSet = setOf(Setting("Property 1", "Value 1"), Setting("Property 2", "Value 2"))

        private val linearId = UniqueIdentifier()
        private val stateRef = StateRef(SecureHash.allOnesHash, 1)

        private val membershipWithInt = Membership(network, alice, intClaimSet, intSettingsSet, linearId, stateRef)
        private val anotherMembershipWithInt = membershipWithInt.copy(holder = bob)
        private val membershipWithString = Membership(network, alice, stringClaimSet, stringSettingsSet, linearId, stateRef)
        private val anotherMembershipWithString = membershipWithString.copy(holder = bob)
        private val membershipWithIntAndString = Membership(network, alice, intClaimSet, stringSettingsSet, linearId, stateRef)
        private val anotherMembershipWithIntAndString = membershipWithIntAndString.copy(holder = bob)

        @JvmStatic
        fun testMemberships() = listOf(
            Arguments.of(
                membershipWithInt,
                anotherMembershipWithInt,
                MembershipSerializer(MyIntSerializer, Int::class, Int.serializer(), Int::class),
                IntArray(0)
            ),
            Arguments.of(
                membershipWithString,
                anotherMembershipWithString,
                MembershipSerializer(MyStringSerializer, String::class, String.serializer(), String::class),
                intArrayOf(7)
            ),
            Arguments.of(
                membershipWithIntAndString,
                anotherMembershipWithIntAndString,
                MembershipSerializer(MyIntSerializer, Int::class, String.serializer(), String::class),
                intArrayOf(7)
            ),
        )
    }
}