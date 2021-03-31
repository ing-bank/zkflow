package com.ing.zknotary.common.serialization

import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serialize
import com.ing.zknotary.common.serialization.bfl.corda.CordaSerializers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Test if value survives serialization/deserialization.
 */
inline fun <reified T : Any> roundTrip(value: T) {
    val serialization = serialize(value, CordaSerializers)
    val deserialization = deserialize<T>(serialization, CordaSerializers)

    deserialization shouldBe value
}

/**
 * Test if serializations of different instances have the same size.
 */
inline fun <reified T : Any> sameSize(value1: T, value2: T) {
    value1 shouldNotBe value2

    val serialization1 = serialize(value1, CordaSerializers)
    val serialization2 = serialize(value2, CordaSerializers)

    serialization1 shouldNotBe serialization2
    serialization1.size shouldBe serialization2.size
}
