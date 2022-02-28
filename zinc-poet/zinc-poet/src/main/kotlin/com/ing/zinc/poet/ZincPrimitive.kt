package com.ing.zinc.poet

import java.util.Locale

enum class ZincPrimitive : ZincType {
    U8,
    U16,
    U24,
    U32,
    U64,
    U128,
    I8,
    I16,
    I32,
    I64,
    I128,
    Bool,
    Unit;

    override fun getId(): String {
        return when (this) {
            Unit -> "()"
            else -> name.toLowerCase(Locale.getDefault())
        }
    }
}
