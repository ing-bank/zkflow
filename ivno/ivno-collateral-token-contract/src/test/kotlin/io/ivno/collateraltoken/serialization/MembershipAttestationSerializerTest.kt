package io.ivno.collateraltoken.serialization

import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.ivno.collateraltoken.zinc.types.anEvenOtherAnonymousParty
import io.ivno.collateraltoken.zinc.types.anotherNetworkWithDifferentOperator
import io.ivno.collateraltoken.zinc.types.attestation
import io.ivno.collateraltoken.zinc.types.copy
import io.ivno.collateraltoken.zinc.types.network
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

    companion object {
        val membershipAttestation = MembershipAttestationSurrogate(
            network, attestation
        ).toOriginal()
        val anotherMembershipAttestation = MembershipAttestationSurrogate(
            anotherNetworkWithDifferentOperator,
            attestation.copy {
                attestor = anEvenOtherAnonymousParty
            }
        ).toOriginal()
    }
}
