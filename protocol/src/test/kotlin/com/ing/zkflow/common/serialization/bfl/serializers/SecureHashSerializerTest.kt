package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.crypto.ZINC
import com.ing.zkflow.testing.assertRoundTripSucceeds
import com.ing.zkflow.testing.assertSameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.Test

class SecureHashSerializerTest {
    @Serializable
    data class Data(val value: @Contextual SecureHash)

    @Serializable
    data class ListData(@FixedLength([3]) val myList: List<@Contextual SecureHash>)

    @Test
    fun `SecureHash serializer`() {
        assertRoundTripSucceeds(SecureHash.allOnesHash)
        assertSameSize(SecureHash.allOnesHash, SecureHash.zeroHash)
    }

    @Test
    fun `SecureHash as part of structure serializer`() {
        assertRoundTripSucceeds(Data(SecureHash.allOnesHash))
        assertSameSize(Data(SecureHash.allOnesHash), Data(SecureHash.zeroHash))
    }

    @Test
    fun `different internal classes of SecureHash in collection should be serialized successfully`() {
        // sealed classes are handled differently to abstract classes
        val listData1 = ListData(
            listOf(
                SecureHash.randomSHA256(),
                SecureHash.random(SecureHash.ZINC),
            )
        )

        val listData2 = ListData(
            listOf(
                SecureHash.randomSHA256(),
            )
        )

        assertRoundTripSucceeds(listData1)
        assertSameSize(listData1, listData2)
    }
}
