package com.ing.zknotary.testing

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.serialization.toObliviousWitness
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

public abstract class DeserializationTestBase<T : DeserializationTestBase<T, D>, D : Any>(
    private val zincJsonSerializer: ZincJsonSerializer<D>
) {

    public abstract fun getZincZKService(): ZincZKService

    @ExperimentalSerializationApi
    public open fun getSerializersModule(): SerializersModule = EmptySerializersModule

    private val zinc by lazy { getZincZKService() }

    @ExperimentalSerializationApi
    @ParameterizedTest
    @MethodSource("testData")
    public fun performDeserializationTest(data: D) {
        val witness = toObliviousWitness(data, getSerializersModule())

        val expected = zincJsonSerializer.toZincJson(data)
        zinc.run(witness, expected)
    }

    public fun interface ZincJsonSerializer<D> {
        public fun toZincJson(data: D): String
    }
}
