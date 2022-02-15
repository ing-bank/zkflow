package com.r3.cbdc.annotated.types

import com.r3.cbdc.annotated.states.EvolvableTokenType
import net.corda.core.contracts.LinearPointer

class TokenPointer<T : EvolvableTokenType>(
    val pointer: LinearPointer<T>,
    fractionDigits: Int
) : TokenType(pointer.pointer.id.toString(), fractionDigits)
