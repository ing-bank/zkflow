package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.testing.roundTrip
import com.ing.zknotary.testing.sameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.UniqueIdentifier
import org.junit.jupiter.api.Test
import java.util.UUID

class UniqueIdentifierSerializerTest {
    @Serializable
    data class Data(@FixedLength([20]) val value: @Contextual UniqueIdentifier)

    @Test
    fun `UniqueIdentifier serializer`() {
        roundTrip(UniqueIdentifier())
        sameSize(UniqueIdentifier(id = UUID(0, 1)), UniqueIdentifier(id = UUID(0, 2)))
    }

    @Test
    fun `UniqueIdentifier as part of structure serializer`() {
        roundTrip(Data(UniqueIdentifier()))
        sameSize(Data(UniqueIdentifier(id = UUID(0, 1))), Data(UniqueIdentifier(id = UUID(0, 2))))
    }
}
