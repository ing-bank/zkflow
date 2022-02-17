package com.ing.zkflow.serialization.bfl

import com.ing.zkflow.common.serialization.BFLSerializationScheme.Companion.ZkCommandDataSerializerMap
import kotlinx.serialization.modules.SerializersModule
import net.corda.testing.core.DummyCommandData

public object TestCordaSerializers {
    init {
        ZkCommandDataSerializerMap.register(DummyCommandData::class, DummyCommandDataSerializer)
    }

    public val module: SerializersModule = SerializersModule {
        // Nothing here yet
    }
}
