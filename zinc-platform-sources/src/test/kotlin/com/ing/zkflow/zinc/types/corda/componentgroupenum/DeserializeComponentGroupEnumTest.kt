package com.ing.zkflow.zinc.types.corda.componentgroupenum

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
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
