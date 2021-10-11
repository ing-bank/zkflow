package io.ivno.collateraltoken.serialization

import com.ing.zkflow.testing.assertRoundTripSucceeds
import com.ing.zkflow.testing.assertSameSize
import io.ivno.collateraltoken.zinc.types.anotherAttestationPointer
import io.ivno.collateraltoken.zinc.types.attestationPointer
import io.onixlabs.corda.identityframework.contract.AttestationPointer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class AttestationPointerSerializerTest {
    @Serializable
    data class Data(val value: @Contextual AttestationPointer<*>)

    private val serializersModule = IvnoSerializers.serializersModule

    @Test
    fun `serialize and deserialize AttestationPointer directly`() {
        assertRoundTripSucceeds(attestationPointer, serializersModule)
        assertSameSize(attestationPointer, anotherAttestationPointer, serializersModule)
    }

    @Test
    fun `serialize and deserialize AttestationPointer`() {
        val data1 = Data(attestationPointer)
        val data2 = Data(anotherAttestationPointer)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}