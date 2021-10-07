package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.zkflow.testing.assertRoundTripSucceeds
import com.ing.zkflow.testing.assertSameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.UniqueIdentifier
import org.junit.jupiter.api.Test
import java.util.UUID

class UniqueIdentifierSerializerTest {
    @Serializable
    data class Data(val value: @Contextual UniqueIdentifier)

    @Test
    fun `UniqueIdentifier serializer`() {
        assertRoundTripSucceeds(UniqueIdentifier())
        assertSameSize(UniqueIdentifier(id = UUID(0, 1)), UniqueIdentifier(id = UUID(0, 2)))
    }

    @Test
    fun `UniqueIdentifier as part of structure serializer`() {
        assertRoundTripSucceeds(Data(UniqueIdentifier()))
        assertSameSize(Data(UniqueIdentifier(id = UUID(0, 1))), Data(UniqueIdentifier(id = UUID(0, 2))))
    }
}
