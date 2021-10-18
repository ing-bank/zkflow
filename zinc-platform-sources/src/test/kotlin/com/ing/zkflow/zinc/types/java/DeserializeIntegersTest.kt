package com.ing.zkflow.zinc.types.java

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toWitness
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.util.UUID

@ExperimentalUnsignedTypes
class DeserializeIntegersTest {
    private val zincZKService = getZincZKService<DeserializeIntegersTest>()

    @Test
    fun `all integer types should be deserialized correctly`() {
        val data = Data(
            Byte.MIN_VALUE, Byte.MAX_VALUE,
            Short.MIN_VALUE, Short.MAX_VALUE,
            Int.MIN_VALUE, Int.MAX_VALUE,
            Long.MIN_VALUE, Long.MAX_VALUE,
        )
        val witness = toWitness(data)

        val expected = data.toZincJson()
        zincZKService.run(witness, expected)
    }

    @Serializable
    private data class Data(
        val i8: Byte,
        val u8: Byte,
        val i16: Short,
        val u16: Short,
        val i32: Int,
        val u32: Int,
        val i64: Long,
        val u64: Long,
        // Java/Kotlin does not support 128 bit integers, use UUID as workaround with limitations
        val i128: @Contextual UUID = UUID(Long.MIN_VALUE, Long.MAX_VALUE),
        val u128: @Contextual UUID = UUID(Long.MAX_VALUE, Long.MIN_VALUE),
    ) {
        fun toZincJson(): String {
            return "{" +
                "\"i_8\": \"$i8\"," +
                "\"u_8\": \"$u8\"," +
                "\"i_16\": \"$i16\"," +
                "\"u_16\": \"$u16\"," +
                "\"i_32\": \"$i32\"," +
                "\"u_32\": \"$u32\"," +
                "\"i_64\": \"$i64\"," +
                "\"u_64\": \"$u64\"," +
                // Java/Kotlin does not support 128 bit integers, and zinc serializes u128 with hex, so hardcode expected value
                "\"i_128\": \"-170141183460469231722463931679029329921\"," +
                "\"u_128\": \"0x7fffffffffffffff8000000000000000\"" +
                "}"
        }
    }
}
