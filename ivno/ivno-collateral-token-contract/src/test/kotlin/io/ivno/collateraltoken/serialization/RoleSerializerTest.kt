package io.ivno.collateraltoken.serialization

import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.onixlabs.corda.bnms.contract.Role
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class RoleSerializerTest {
    @Serializable
    data class Data(val value: @Contextual Role)

    private val role1 = Role("Thief")
    private val role2 = Role("Warrior")

    private val serializersModule = IvnoSerializers.serializersModule

    @Test
    fun `serialize and deserialize Contract-Role directly`() {
        assertRoundTripSucceeds(role1, serializersModule)
        assertSameSize(role1, role2, serializersModule)
    }

    @Test
    fun `serialize and deserialize Contract-Role`() {
        val data1 = Data(role1)
        val data2 = Data(role2)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}