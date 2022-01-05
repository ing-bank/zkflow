package com.ing.zkflow.annotated.pilot.r3.types

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import net.corda.core.identity.Party

// Open classes may be extended provided no values are overridden.
// Otherwise, serialization breaks.
// see, https://github.com/Kotlin/kotlinx.serialization/issues/1792

@ZKP
data class IssuedTokenType(
    val issuer: @EdDSA Party,
    val tokenType: TokenType
) : TokenType(tokenType.tokenIdentifier, tokenType.fractionDigits)
