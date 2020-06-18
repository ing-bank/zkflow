package com.ing.zknotary.common.serializer

import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.serialization.SingletonSerializeAsToken

interface SerializationFactoryService : SerializeAsToken {
    val factory: SerializationFactory
}

@CordaService
class ZKJsonSerializationFactoryService() : SingletonSerializeAsToken(), SerializationFactoryService {

    // For CordaService. We don't need the serviceHub anyway in this Service
    constructor(serviceHub: AppServiceHub?) : this()

    override val factory: SerializationFactory
        get() = ZKJsonSerializationFactory
}

@CordaService
class ZincSerializationFactoryService() :
    SingletonSerializeAsToken(),
    SerializationFactoryService {

    // For CordaService. We don't need the serviceHub anyway in this Service
    constructor(serviceHub: AppServiceHub?) : this()

    override val factory: SerializationFactory
        get() = ZincSerializationFactory
}
