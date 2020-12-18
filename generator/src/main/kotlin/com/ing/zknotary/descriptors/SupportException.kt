package com.ing.zknotary.descriptors

import com.google.devtools.ksp.symbol.KSType
import java.lang.Exception

sealed class SupportException(message: String) : Exception(message) {
    class UnsupportedNativeType(type: KSType, supportedTypes: List<String>) : SupportException(
        listOf(
            "No built-in support for $type.",
            "Supported types:",
            supportedTypes.joinToString(separator = ", ")
        ).joinToString(separator = " ", postfix = ".")
    )

    class UnsupportedUserType(type: KSType, supportedUserTypes: List<String>) : SupportException(
        listOf(
            "Type $type is neither annotated with `Sized` nor with `UseDefault`.",
            "Supported user types:",
            supportedUserTypes.joinToString(separator = ", ")
        ).joinToString(separator = " ", postfix = ".")
    )
}
