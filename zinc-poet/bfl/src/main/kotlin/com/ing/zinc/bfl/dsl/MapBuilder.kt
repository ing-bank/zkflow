package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflMap
import com.ing.zinc.bfl.BflType

class MapBuilder {
    var capacity: Int? = null
    var keyType: BflType? = null
    var valueType: BflType? = null

    fun build() = BflMap(
        requireNotNull(capacity) { "Map property capacity is missing" },
        requireNotNull(keyType) { "Map property keyType is missing" },
        requireNotNull(valueType) { "Map property valueType is missing" }
    )

    companion object {
        fun map(init: MapBuilder.() -> Unit): BflMap = MapBuilder().apply(init).build()
    }
}
