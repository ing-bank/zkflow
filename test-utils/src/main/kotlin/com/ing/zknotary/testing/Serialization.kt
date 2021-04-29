package com.ing.zknotary.testing

import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

/**
 * Test if value survives serialization/deserialization.
 */
public inline fun <reified T : Any> assertRoundTripSucceeds(
    value: T,
    serializers: SerializersModule = EmptySerializersModule,
    strategy: KSerializer<T>? = null
): T {
    val serialization = serialize(value, strategy, serializersModule = CordaSerializers + serializers)
    val deserialization = deserialize<T>(serialization, strategy, serializersModule = CordaSerializers + serializers)

    deserialization shouldBe value
    return deserialization
}

/**
 * Test if serializations of different instances have the same size.
 */
public inline fun <reified T : Any> assertSameSize(
    value1: T,
    value2: T,
    serializers: SerializersModule = EmptySerializersModule,
    strategy: KSerializer<T>? = null
) {
    value1 shouldNotBe value2

    val serialization1 = serialize(value1, strategy, serializersModule = CordaSerializers + serializers)
    val serialization2 = serialize(value2, strategy, serializersModule = CordaSerializers + serializers)

    serialization1 shouldNotBe serialization2
    serialization1.size shouldBe serialization2.size
}
