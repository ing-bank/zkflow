package io.ivno.collateraltoken.serialization

import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.onixlabs.corda.bnms.contract.Permission
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test

class PermissionSerializerTest {
    @Serializable
    data class Data(val value: @Contextual Permission)

    private val permission1 = Permission("Permission1")
    private val permission2 = Permission("Permission2")

    private val serializersModule = SerializersModule { contextual(PermissionSerializer) }

    @Test
    fun `serialize and deserialize Contract-Permission directly`() {
        assertRoundTripSucceeds(permission1, serializersModule)
        assertSameSize(permission1, permission2, serializersModule)
    }

    @Test
    fun `serialize and deserialize Contract-Permission`() {
        val data1 = Data(permission1)
        val data2 = Data(permission2)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}