package com.ing.zknotary.zinc.types.corda.componentgroupenum

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import net.corda.core.contracts.ComponentGroupEnum

class DeserializeComponentGroupEnumTest :
    DeserializationTestBase<DeserializeComponentGroupEnumTest, ComponentGroupEnum>(
        { it.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeComponentGroupEnumTest>()

    companion object {
        @JvmStatic
        fun testData() = ComponentGroupEnum.values()
    }
}
