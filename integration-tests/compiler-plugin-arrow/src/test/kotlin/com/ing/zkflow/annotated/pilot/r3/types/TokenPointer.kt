package com.ing.zkflow.annotated.pilot.r3.types

import com.ing.zkflow.annotated.pilot.r3.states.EvolvableTokenType
import net.corda.core.contracts.LinearPointer

class TokenPointer<T : EvolvableTokenType>(
    val pointer: LinearPointer<T>,
    fractionDigits: Int
) : TokenType(pointer.pointer.id.toString(), fractionDigits)
