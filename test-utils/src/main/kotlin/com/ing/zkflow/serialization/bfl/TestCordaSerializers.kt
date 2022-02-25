package com.ing.zkflow.serialization.bfl

import com.ing.zkflow.common.serialization.BFLSerializationScheme.Companion.ZkCommandDataSerializerMap
import com.ing.zkflow.util.tryNonFailing
import kotlinx.serialization.modules.SerializersModule
import net.corda.testing.core.DummyCommandData

public object TestCordaSerializers {
    init {
        tryNonFailing {
            ZkCommandDataSerializerMap.register(DummyCommandData::class, DummyCommandDataSerializer)
        }
    }

    public val module: SerializersModule = SerializersModule {
        // Nothing here yet
    }
}
