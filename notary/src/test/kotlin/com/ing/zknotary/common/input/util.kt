package com.ing.zknotary.common.input

import com.ing.zknotary.common.serializer.jackson.zinc.ZincJacksonSupport

val objectMapper = ZincJacksonSupport.createDefaultMapper()

fun <T : Any> T.toJSON(): String {
    return objectMapper.writeValueAsString(this)
}
