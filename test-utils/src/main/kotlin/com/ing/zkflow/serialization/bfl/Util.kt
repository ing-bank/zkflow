package com.ing.zkflow.serialization.bfl

import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Test if value survives serialization/deserialization.
 */
public inline fun <reified T : Any> assertRoundTripSucceeds(
    value: T,
    serializers: SerializersModule = EmptySerializersModule,
    strategy: KSerializer<T>? = null,
    outerFixedLength: IntArray = IntArray(0)
): T {
    val serialization = com.ing.serialization.bfl.api.reified.serialize(
        value,
        strategy,
        serializersModule = CordaSerializers.module + serializers,
        outerFixedLength
    )
    val deserialization =
        deserialize<T>(serialization, strategy, serializersModule = CordaSerializers.module + serializers, outerFixedLength)

    // deserialization shouldBe value
    assertEquals(value, deserialization)
    return deserialization
}

/**
 * Test if serializations of different instances have the same size.
 */
public inline fun <reified T : Any> assertSameSize(
    value1: T,
    value2: T,
    serializers: SerializersModule = EmptySerializersModule,
    strategy: KSerializer<T>? = null,
    outerFixedLength: IntArray = IntArray(0)
) {
    assertNotEquals(value2, value1)
    // value1 shouldNotBe value2

    val serialization1 = com.ing.serialization.bfl.api.reified.serialize(
        value1,
        strategy,
        serializersModule = CordaSerializers.module + serializers,
        outerFixedLength
    )
    val serialization2 = com.ing.serialization.bfl.api.reified.serialize(
        value2,
        strategy,
        serializersModule = CordaSerializers.module + serializers,
        outerFixedLength
    )

    // serialization1 shouldNotBe serialization2
    assertNotEquals(serialization2, serialization1)
    // serialization1.size shouldBe serialization2.size
    assertEquals(serialization1.size, serialization2.size)
}
