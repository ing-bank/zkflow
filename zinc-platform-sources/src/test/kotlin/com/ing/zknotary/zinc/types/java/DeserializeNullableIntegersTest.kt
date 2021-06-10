package com.ing.zknotary.zinc.types.java

import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toWitness
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class DeserializeNullableIntegersTest {
    private val zincZKService = getZincZKService<DeserializeNullableIntegersTest>()

    @Test
    fun `all nullable integer types should be deserialized correctly`() {
        val data = Data(
            Byte.MIN_VALUE, Byte.MAX_VALUE,
            Short.MIN_VALUE, Short.MAX_VALUE,
            Int.MIN_VALUE, Int.MAX_VALUE,
            Long.MIN_VALUE, Long.MAX_VALUE,
        )
        val witness = toWitness(data)

        val expected = toZincJson(data)
        zincZKService.run(witness, expected)
    }

    @Serializable
    data class Data(
        val i8: Byte?,
        val u8: Byte?,
        val i16: Short?,
        val u16: Short?,
        val i32: Int?,
        val u32: Int?,
        val i64: Long?,
        val u64: Long?,
        // Java/Kotlin does not support 128 bit integers, use UUID as workaround with limitations
        val i128: @Contextual UUID? = UUID(Long.MIN_VALUE, Long.MAX_VALUE),
        val u128: @Contextual UUID? = UUID(Long.MAX_VALUE, Long.MIN_VALUE),
    )

    fun toZincJson(data: Data): String =
        listOf("i8", "u8", "i16", "u16", "i32", "u32", "i64", "u64", "i128", "u128").joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}"
        ) { propertyName ->
            "\"${propertyName[0]}_${propertyName.substring(1)}\": " + buildJsonObject {
                val inner = when (propertyName) {
                    "i128" -> "-170141183460469231722463931679029329921"
                    "u128" -> "0x7fffffffffffffff8000000000000000"
                    else -> "${(data::class.memberProperties.first { it.name == propertyName } as KProperty1<Data, *>).get(data)}"
                }

                put("is_null", false)
                put("inner", inner)
            }.toString()
        }
}
