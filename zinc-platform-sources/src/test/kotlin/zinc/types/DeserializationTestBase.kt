package zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

abstract class DeserializationTestBase<T : DeserializationTestBase<T, D>, D : Any>(
    private val zincJsonSerializer: ZincJsonSerializer<D>
) {

    abstract fun getZincZKService(): ZincZKService

    open fun getSerializersModule(): SerializersModule = EmptySerializersModule

    private val zinc by lazy { getZincZKService() }

    @ParameterizedTest
    @MethodSource("testData")
    fun performDeserializationTest(data: D) {
        val witness = toObliviousWitness(data, getSerializersModule())

        val expected = zincJsonSerializer.toZincJson(data)
        zinc.run(witness, expected)
    }

    fun interface ZincJsonSerializer<D> {
        fun toZincJson(data: D): String
    }
}
