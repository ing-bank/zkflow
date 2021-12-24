package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import kotlin.reflect.KClass

interface ZincTypeResolver {
    fun zincTypeOf(kClass: KClass<*>): BflModule
}
