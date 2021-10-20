package com.ing.zkflow.serialization.bfl

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.testing.core.DummyCommandData

@Serializable
@SerialName("x")
public object DummyCommandDataSurrogate : Surrogate<DummyCommandData> {
    override fun toOriginal(): DummyCommandData = DummyCommandData
}

public object DummyCommandDataSerializer : SurrogateSerializer<DummyCommandData, DummyCommandDataSurrogate>(
    strategy = DummyCommandDataSurrogate.serializer(),
    toSurrogate = { DummyCommandDataSurrogate }
)
