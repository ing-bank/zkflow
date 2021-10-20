package com.ing.zkflow.zinc.types.java.collection.int

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Serializable

class DeserializeCollectionTest : DeserializationTestBase <DeserializeCollectionTest, DeserializeCollectionTest.Data>(
    {
        it.data.toZincJson(FIXED_LIST_SIZE)
    }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeCollectionTest>()

    @Serializable
    data class Data(@FixedLength([FIXED_LIST_SIZE]) val data: List<Int>)

    companion object {
        const val FIXED_LIST_SIZE = 3

        @JvmStatic
        fun testData() = listOf(
            Data(listOf(1, 2)),
        )
    }
}
