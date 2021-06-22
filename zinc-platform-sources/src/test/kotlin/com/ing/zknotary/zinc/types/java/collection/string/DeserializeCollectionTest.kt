package com.ing.zknotary.zinc.types.java.collection.string

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Serializable

class DeserializeCollectionTest :
    DeserializationTestBase<DeserializeCollectionTest, DeserializeCollectionTest.Data>(
        {
            it.data.toZincJson(FIXED_LIST_SIZE, FIXED_STRING_SIZE)
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeCollectionTest>()

    @Serializable
    data class Data(@FixedLength([FIXED_LIST_SIZE, FIXED_STRING_SIZE]) val data: List<String>)

    companion object {
        const val FIXED_LIST_SIZE = 3
        const val FIXED_STRING_SIZE = 1

        @JvmStatic
        fun testData() = listOf(
            Data(listOf("1", "2")),
        )
    }
}
