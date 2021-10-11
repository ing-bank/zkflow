package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import io.ivno.collateraltoken.zinc.types.anotherMembershipWithInt
import io.ivno.collateraltoken.zinc.types.anotherMembershipWithString
import io.ivno.collateraltoken.zinc.types.anotherMembershipWithIntAndString
import io.ivno.collateraltoken.zinc.types.membershipWithInt
import io.ivno.collateraltoken.zinc.types.membershipWithString
import io.ivno.collateraltoken.zinc.types.membershipWithIntAndString
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
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