package com.ing.zkflow.serialization.bfl

import com.ing.zkflow.serialization.CommandDataSerializerMap
import kotlinx.serialization.modules.SerializersModule
import net.corda.testing.core.DummyCommandData

public object TestCordaSerializers {
    init {
        CommandDataSerializerMap.register(DummyCommandData::class, DummyCommandDataSerializer)
    }

    public val module: SerializersModule = SerializersModule {
        // Nothing here yet
    }
}
