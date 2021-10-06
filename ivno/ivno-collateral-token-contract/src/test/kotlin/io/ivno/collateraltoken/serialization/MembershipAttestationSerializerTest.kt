package io.ivno.collateraltoken.serialization

import com.ing.zkflow.testing.assertRoundTripSucceeds
import com.ing.zkflow.testing.assertSameSize
import io.ivno.collateraltoken.zinc.types.anotherMembershipAttestation
import io.ivno.collateraltoken.zinc.types.membershipAttestation
import org.junit.jupiter.api.Test

internal class MembershipAttestationSerializerTest {
    @Test
    fun `roundtrip should succeed`() {
        assertRoundTripSucceeds(
            membershipAttestation,
            serializers = IvnoSerializers.serializersModule
        )
    }

    @Test
    fun `sizes should be equal`() {
        assertSameSize(
            membershipAttestation,
            anotherMembershipAttestation,
            serializers = IvnoSerializers.serializersModule
        )
    }
}
